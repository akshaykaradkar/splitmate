/**
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import * as fs from 'fs';
import * as path from 'path';
import * as util from 'util';

interface TokenSet {
  // The token set name prefix. Ex: "md.comp.button"
  prefix: string;
  // A map of display groups in the token set and the tokens they contain.
  displayGroups: Record<string, string[]>;
  // A map of tokens in the token set, keyed by the token name.
  tokens: Record<string, Token>;
}

interface Token {
  // The token set name prefix this token belongs to. Ex: "md.comp.button"
  prefix: string;
  // Optional human-readable display group this token belongs to. Ex: "Container"
  displayGroup?: string;
  // The name of the token. Ex: "md.comp.button.container.color"
  name: string;
  // Optional human-readable description of the token.
  description?: string;
  // Whether the token is deprecated.
  deprecated: boolean;
  // Optional deprecation message for the token.
  deprecationMessage?: string;
  // The name of the Sass module, corresponding to the file name. Ex: "md-comp-button"
  sassModule: string;
  // The Sass name of the token. Ex: "$container-color" (variable) or "label-text" (mixin)
  sassName: string;
  // The Sass value of the token. Ex: "md-sys-color.$primary" (variable) or "md-sys-typescale.label-large" (mixin)
  sassValue: string;
  // Indicates the token is a Sass mixin when true, or a Sass variable when false.
  isSassMixin: boolean;
}

/**
 * Parses a Sass file and returns a TokenSet object containing the tokens and
 * display groups found in the file.
 *
 * @param sassFilePath The path to the Sass file.
 * @param content The contents of the Sass file.
 * @return A TokenSet object containing the tokens and display groups found in
 *     the Sass file.
 */
export function parseSassTokens(
  sassFilePath: string,
  content: string,
): TokenSet {
  // First determine the prefix of the token set. The Sass file name is equal
  // to the preset, but separated by dashes instead of periods. This can make
  // it difficult to determine the prefix if the name of a component has a dash.
  //
  // To determine the name, the first token comment is compared to the file name
  // parts to determine the prefix.
  //
  // Example:
  //   Sass file name: _md-comp-outlined-card.scss
  //   Token found in file: `/// md.comp.outlined-card.container.color`
  //   Prefix: `md.comp.outlined-card`, not `md.comp.outlined.card`
  const sassModule = path.basename(sassFilePath, '.scss').substring(1);
  let prefix = '';
  const firstTokenMatch = content.match(
    /^\/\/\/ ([\w.-]+)[^\n]*\n(?:[^\n]*\n)*?\$([\w-]+):/m,
  );
  if (!firstTokenMatch) {
    throw new Error(
      `Sass file ${path.basename(sassFilePath)} does not contain Sassdoc comments with token names. (Ex: \`/// md.comp.foo \`)`,
    );
  }

  const sassModuleParts = sassModule.split('-');
  const firstTokenParts = firstTokenMatch[1].split('.');
  const prefixParts: string[] = [];
  let sassModulePartIndex = 0;
  for (const tokenPart of firstTokenParts) {
    if (sassModulePartIndex >= sassModuleParts.length) {
      // We've consumed all sass module parts, so we're beyond the prefix.
      break;
    }
    // Sass module: `md-comp-outlined-card`
    // Token name:  `md.comp.outlined-card.container.color`
    const sassModulePart = sassModuleParts[sassModulePartIndex];
    // If the parts match (ex: "md" or "comp"), add the part to the prefix and
    // continue to the next part.
    if (sassModulePart === tokenPart) {
      sassModulePartIndex++;
      prefixParts.push(tokenPart);
      continue;
    }

    // If the parts do not match, check if it is a dash-separated part that
    // matches the next Sass module parts.
    if (tokenPart.includes('-')) {
      const dashTokenParts = tokenPart.split('-');
      let matches =
        sassModulePartIndex + dashTokenParts.length <= sassModuleParts.length;
      if (matches) {
        for (let i = 0; i < dashTokenParts.length; i++) {
          if (sassModuleParts[sassModulePartIndex + i] !== dashTokenParts[i]) {
            matches = false;
            break;
          }
        }
      }

      if (matches) {
        sassModulePartIndex += dashTokenParts.length;
        prefixParts.push(tokenPart);
        continue;
      }
    }

    // The parts do not match. This may mean the end of the token prefix, or
    // that the token parts do not match a prefix for the Sass module.
    break;
  }

  if (sassModulePartIndex !== sassModuleParts.length) {
    // The script did not fully match the Sass module prefix to the token names.
    throw new Error(
      `Sass file ${path.basename(sassFilePath)} does not match as a prefix for the first token found \`${firstTokenMatch[1]}\``,
    );
  }

  prefix = prefixParts.join('.');

  // Parse each line of the Sass file, extracting token Sassdoc comments and
  // the Sass variables or mixins associated with them.
  //
  // Example:
  // ```scss
  // /// md.comp.button.hovered.container.elevation (Hovered)
  // ///
  // /// @deprecated No longer part of the design spec
  // $hovered-container-elevation: md-sys-elevation.$level1;
  // /// md.comp.button.unselected.pressed.state-layer.color (Pressed)
  // $unselected-pressed-state-layer-color: md-sys-color.$on-surface-variant;
  // /// md.comp.button.label-text
  // @mixin label-text {
  //   @include md-sys-typescale.label-large;
  // }
  // ```
  const tokens: Record<string, Token> = {};
  const displayGroups: Record<string, string[]> = {};
  let sassdocCommentLines: string[] = [];

  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Collect preceeding Sassdoc comment lines before the Sass variable or
    // mixin.
    if (line.startsWith('///')) {
      sassdocCommentLines.push(line.substring(3).trim());
      continue;
    }

    // A function that parses the collected Sassdoc comment lines for a variable
    // or mixin.
    const parsesassdocCommentLines = (): Pick<
      Token,
      | 'name'
      | 'displayGroup'
      | 'description'
      | 'deprecated'
      | 'deprecationMessage'
    > => {
      // Match the token name and display group from the first Sassdoc comment.
      const tokenNameMatch = sassdocCommentLines[0]?.match(
        /^([\w.-]+)(?:\s+\((.*)\))?$/,
      );
      if (!tokenNameMatch) {
        throw new Error(
          `Sass file ${path.basename(sassFilePath)} contains a Sass variable that does not have a token name in its Sassdoc comment. Line ${i + 1}:\n\`${line}\``,
        );
      }

      const partialToken: Pick<
        Token,
        | 'name'
        | 'displayGroup'
        | 'description'
        | 'deprecated'
        | 'deprecationMessage'
      > = {
        name: tokenNameMatch[1],
        deprecated: false,
      };
      if (tokenNameMatch[2]) {
        partialToken.displayGroup = tokenNameMatch[2];
      }

      // For the rest of each comment line, collect the description and
      // deprecation information.
      const descriptionLines: string[] = [];
      for (const commentLine of sassdocCommentLines.slice(1)) {
        const deprecatedMatch = commentLine.match(/^@deprecated\s?(.*)/);
        if (deprecatedMatch) {
          partialToken.deprecated = true;
          partialToken.deprecationMessage = deprecatedMatch[1];
        } else {
          descriptionLines.push(commentLine);
        }
      }
      if (descriptionLines.length) {
        partialToken.description = descriptionLines.join('\n');
      }

      // Clear the collected Sassdoc comment lines.
      sassdocCommentLines = [];
      return partialToken;
    };

    // Parse a Sass variable.
    if (line.startsWith('$')) {
      // Match a Sass variable declaration, such as:
      // `$hovered-container-elevation: md-sys-elevation.$level1;`
      const match = line.match(/^\$([\w-]+):\s*([^;]+);/);
      if (!match) {
        throw new Error(
          `Sass file ${path.basename(sassFilePath)} contains a Sass variable that could not be parsed. Line ${i + 1}:\n\`${line}\``,
        );
      }

      const sassName = `$${match[1]}`;
      const sassValue = match[2];
      const token: Token = {
        prefix,
        ...parsesassdocCommentLines(),
        sassModule,
        sassName,
        sassValue,
        isSassMixin: false,
      };

      tokens[token.name] = token;
      if (token.displayGroup) {
        displayGroups[token.displayGroup] ??= [];
        displayGroups[token.displayGroup].push(token.name);
      }
      continue;
    }

    // Parse a Sass mixin.
    if (line.startsWith('@mixin')) {
      const mixinMatch = line.match(/^@mixin\s+([\w-]+)/);
      if (!mixinMatch) {
        throw new Error(
          `Sass file ${path.basename(sassFilePath)} contains a Sass mixin that could not be parsed. Line ${i + 1}:\n\`${line}\``,
        );
      }
      const mixinName = mixinMatch[1];
      // Parse the composite token's mixin content. We only care about parsing
      // reference composite tokens, mixins with an `@include` reference to
      // another token.
      //
      // Example:
      // ```scss
      // @mixin label-text {
      //   @include md-sys-typescale.label-large;
      // }
      // ```
      //
      // Skip mixins that are local composite tokens referencing other token
      // variables within the token set.
      // Example:
      // ```scss
      // $label-text-font: md-sys-typescale.$label-large-font;
      // $label-text-size: md-sys-typescale.$label-large-size;
      // $label-text-weight: md-sys-typescale.$label-large-weight;
      // $label-text-tracking: md-sys-typescale.$label-large-tracking;
      // $label-text-line-height: md-sys-typescale.$label-large-line-height;
      // // Do not include this local composite token that references other
      // // variables that are already included in the token set.
      // @mixin label-text {
      //   font-family: $label-text-font;
      //   font-size: $label-text-size;
      //   font-weight: $label-text-weight;
      //   letter-spacing: $label-text-tracking;
      //   line-height: $label-text-line-height;
      // }
      // ```
      let mixinContent = '';
      const includeLine = lines[i + 1];
      const includeMatch = includeLine?.match(/@include\s+([^;]+);/);
      if (includeMatch) {
        mixinContent = includeMatch[1];
        i += 2; // skip include line and closing brace line
      } else {
        // Skip mixins with CSS declarations that do not @include another
        // token reference, and clear the collected Sassdoc comment lines.
        sassdocCommentLines = [];
        continue;
      }

      const token: Token = {
        prefix,
        ...parsesassdocCommentLines(),
        sassModule,
        sassName: mixinName,
        sassValue: mixinContent,
        isSassMixin: true,
        deprecated: false,
      };

      tokens[token.name] = token as Token;
      if (token.displayGroup) {
        displayGroups[token.displayGroup] ??= [];
        displayGroups[token.displayGroup].push(token.name);
      }
      continue;
    }

    if (sassdocCommentLines.length > 0 && line.trim() !== '') {
      throw new Error(
        `Sass file ${path.basename(sassFilePath)} contains a line that is not a comment or mixin, but is preceeded by Sassdoc comments. Line ${i + 1}:\n\`${line}\``,
      );
    }
  }

  return {
    prefix,
    displayGroups,
    tokens,
  };
}

interface CascadingTokenSet {
  prefix: string;
  configs: Array<string | string[]>;
  states: Array<string | string[]>;
  properties: string[];
  values: Record<string, Record<string, string>>;
}

// Known property parts whose names contain parts that match layer names but
// should not be removed when determining properties vs layers.
const KNOWN_PROPERTY_PARTS = ['focus.indicator'];

function cascadeTokenSets(
  tokenSets: TokenSet[],
  configs: string[][],
  states: string[][],
  prefix?: string,
): CascadingTokenSet {
  const layers: string[] = [...configs.flat(), ...states.flat()];
  // Collect all prefixes and tokens from each token set.
  const prefixes: string[] = [];
  const allTokens = tokenSets.reduce(
    (tokens, tokenSet) => {
      prefixes.push(tokenSet.prefix);
      return {
        ...tokens,
        ...tokenSet.tokens,
      };
    },
    {} as Record<string, Token>,
  );

  // Sort the prefixes and use the first as the shared common prefix.
  // Cascading token sets should all belong to the same common token set prefix.
  //
  // Ex:
  //   - md.comp.button
  //   - md.comp.button.filled
  //   - md.comp.button.small
  //
  // An error is thrown if the provided token sets belong to different prefixes.
  prefixes.sort();
  prefix ??= prefixes[0];
  for (const variantPrefix of prefixes.slice(1)) {
    if (!variantPrefix.startsWith(prefix)) {
      throw new Error(
        `Token set \`${variantPrefix}\` does not start with the common token set prefix \`${prefix}\`.\n\nIf these token sets do not have a common token set, provide --prefix="..." to the command.`,
      );
    }
  }

  // Build a set of all token "properties": the part of a token for the element
  // and property that is configured, with the prefix and all state layers and
  // configurations removed.
  //
  // For example, these tokens represent the same `container.color` property for
  // different states and configurations:
  //   - md.comp.button.filled.container.color
  //   - md.comp.button.filled.selected.container.color
  //   - md.comp.button.filled.disabled.container.color
  const properties = new Set<string>();
  const propertyTokens: Record<string, string[]> = {};
  const layersFound = new Set<string>();
  for (const [tokenName, token] of Object.entries(allTokens)) {
    if (token.deprecated) {
      continue;
    }

    // For each token, we can remove the prefix and each part that represents
    // a state or configuration (ex: ".hovered.", ".filled.", or ".small.").
    // Doing so will leave just the token element and property parts.
    let propertyName = tokenName;

    // First, try removing each known layer part.
    //
    // Sometimes, layer parts like "focus" are part of a known property, such as
    // "focus.indicator". The script should verify that if this token has a
    // known property part, that we do not accidentally remove it while
    // removing layer parts.
    const knownPropertyPart = KNOWN_PROPERTY_PARTS.find((propertyPart) =>
      tokenName.includes(propertyPart),
    );
    for (const layer of layers) {
      const layerPart = `.${layer}.`;
      if (!propertyName.includes(layerPart)) {
        continue;
      }

      if (knownPropertyPart && propertyName.includes(knownPropertyPart)) {
        // Try removing the part and see if the known property part is still
        // included in the token name.
        const propertyNameWithoutPart = propertyName.replace(layerPart, '.');
        // If removing the layer broke the known property part, do not remove
        // it and continue to the next layer part.
        if (!propertyNameWithoutPart.includes(knownPropertyPart)) {
          continue;
        }
      }

      propertyName = propertyName.replace(layerPart, '.');
      layersFound.add(layer);
    }

    // Finally, remove the common token set prefix from the token name.
    // Only the common prefix needs to be removed, since any other variant
    // prefixes should have been removed as part of the known layer parts.
    propertyName = propertyName.replace(`${prefix}.`, '');
    properties.add(propertyName);
    propertyTokens[propertyName] ??= [];
    propertyTokens[propertyName].push(tokenName);
  }

  const flatConfigs = configs.flat();
  const flatStates = states.flat();
  const sortLayer = (layerA: string, layerB: string) => {
    const isLayerAConfig = flatConfigs.includes(layerA);
    const isLayerBConfig = flatConfigs.includes(layerB);
    if (isLayerAConfig && !isLayerBConfig) {
      return -1;
    }
    if (!isLayerAConfig && isLayerBConfig) {
      return 1;
    }
    if (isLayerAConfig && isLayerBConfig) {
      return flatConfigs.indexOf(layerA) - flatConfigs.indexOf(layerB);
    }

    const isLayerAState = flatStates.includes(layerA);
    const isLayerBState = flatStates.includes(layerB);
    if (isLayerAState && !isLayerBState) {
      return -1;
    }
    if (!isLayerAState && isLayerBState) {
      return 1;
    }
    if (isLayerAState && isLayerBState) {
      return flatStates.indexOf(layerA) - flatStates.indexOf(layerB);
    }

    return 0;
  };

  // Sort tokens by the order in which layers should be cascaded.
  //
  // This sorts by default layers and states first, then configs and their
  // states.
  const sortByCascade = (tokenA: string, tokenB: string) => {
    const tokenAParts = tokenA.split('.');
    const tokenBParts = tokenB.split('.');

    const tokenAConfigs = tokenAParts
      .filter((part) => flatConfigs.includes(part))
      .sort(sortLayer);
    const tokenBConfigs = tokenBParts
      .filter((part) => flatConfigs.includes(part))
      .sort(sortLayer);
    const tokenAStates = tokenAParts
      .filter((part) => flatStates.includes(part))
      .sort(sortLayer);
    const tokenBStates = tokenBParts
      .filter((part) => flatStates.includes(part))
      .sort(sortLayer);

    // Compare config items up to the shared length
    for (
      let i = 0;
      i < Math.min(tokenAConfigs.length, tokenBConfigs.length);
      i++
    ) {
      const result = sortLayer(tokenAConfigs[i], tokenBConfigs[i]);
      if (result !== 0) {
        return result;
      }
    }

    // If shared elements are equal, shorter config set comes first
    if (tokenAConfigs.length < tokenBConfigs.length) {
      return -1;
    }
    if (tokenAConfigs.length > tokenBConfigs.length) {
      return 1;
    }

    // Compare state items up to the shared length
    for (
      let i = 0;
      i < Math.min(tokenAStates.length, tokenBStates.length);
      i++
    ) {
      const result = sortLayer(tokenAStates[i], tokenBStates[i]);
      if (result !== 0) {
        return result;
      }
    }

    // If shared elements are equal, shorter state set comes first
    if (tokenAStates.length < tokenBStates.length) {
      return -1;
    }
    if (tokenAStates.length > tokenBStates.length) {
      return 1;
    }

    // Same config and state, sort by property name.
    return tokenA.localeCompare(tokenB);
  };

  const sortedPropertyValues: Record<string, Record<string, string>> = {};
  for (const property of [...properties].sort()) {
    const sortedTokenValues = propertyTokens[property]
      .toSorted(sortByCascade)
      .reduce(
        (tokens, name) => {
          tokens[name] = allTokens[name].sassValue;
          return tokens;
        },
        {} as Record<string, string>,
      );

    sortedPropertyValues[property] = sortedTokenValues;
  }

  return {
    prefix,
    configs,
    states,
    properties: Object.keys(sortedPropertyValues).sort(),
    values: sortedPropertyValues,
  };
}

// Run the script to parse and cascade the provided token sets.
const {values: argValues, positionals: sassTokenSetFilePaths} = util.parseArgs({
  args: process.argv.slice(2),
  options: {
    'configs': {
      type: 'string',
    },
    'states': {
      type: 'string',
    },
    'prefix': {
      type: 'string',
    },
    'pwd': {
      type: 'string',
    },
    'help': {
      type: 'boolean',
      short: 'h',
    },
  },
  allowPositionals: true,
});

const USAGE = `Usage: node scripts/parse-sass-tokens.js --configs="config1,config2|config3" --states="state1,state2|state3" [--pwd=$PWD] <path/to/_token-set.scss>...`;
if (argValues.help) {
  console.log(USAGE);
  process.exit(0);
}
if (!sassTokenSetFilePaths.length) {
  throw new Error(
    `Missing one or more token set file paths from args.\n\n${USAGE}`,
  );
}

const configs = (argValues.configs ?? '')
  .split(',')
  .map((group) => group.split('|').map((config) => config.trim()));
const states = (argValues.states ?? '')
  .split(',')
  .map((group) => group.split('|').map((state) => state.trim()));

const tokenSets: TokenSet[] = [];
for (const sassFilePath of sassTokenSetFilePaths) {
  const resolvedPath = argValues.pwd
    ? path.resolve(argValues.pwd, sassFilePath)
    : sassFilePath;
  const content = fs.readFileSync(resolvedPath, 'utf-8');
  const tokenSet = parseSassTokens(resolvedPath, content);
  tokenSets.push(tokenSet);
}

const cascadingTokenSets = cascadeTokenSets(
  tokenSets,
  configs,
  states,
  argValues.prefix,
);
console.log(JSON.stringify(cascadingTokenSets, null, 2));
