"""Presubmit check verifying all gBreeze components in //third_party/javascript/material/web/labs/gb/BUILD are documented in GenUX and gBreeze skills."""

from collections.abc import Sequence
import os
import re
import sys

from absl import app
from google3.devtools.api.source import presubmit_stubby_service_pb2
from google3.devtools.api.source.presubmit_build_target.python import presubmit_utils
from google3.pyglib import resources

_LABS_GB_BUILD_DEPOT_PATH = (
    "//depot/google3/third_party/javascript/material/web/labs/gb/BUILD"
)

_SKILL_DEPOT_PATHS = (
    "//depot/google3/ux/gdp/ai/plugins/material_genux_kit/skills/genux/SKILL.md",
    "//depot/google3/ux/gdp/ai/plugins/material_genux_kit/skills/genux/assets/starter_template.html",
    "//depot/google3/ux/gdp/ai/plugins/material_genux_kit/skills/genux/references/genui.md",
    "//depot/google3/ux/gdp/ai/skills/gbreeze/SKILL.md",
)

# Maps component directory names under labs/gb/components/ to their custom
# element tag name when the tag is not simply `md-gb-<dirname>`.
_COMPONENT_DIR_TO_TAG = {
    "appbar": "md-gb-app-bar",
    "focus": "focus-ring",
    "iconbutton": "md-gb-icon-button",
    "navbar": "md-gb-nav-bar",
    "ripple": "ripple",
    "splitbutton": "md-gb-split-button",
}

_CATALOG_ENTRY_RE = re.compile(
    r"//third_party/javascript/material/web/labs/gb/components/"
    r"(?:google3/)?([^/]+)/demo:demo\.catalog_entry"
)


def _read_file_content(depot_path: str) -> str:
  """Reads file content from presubmit client test data path or runfiles."""
  client_path = presubmit_utils.GetTestDataPath(depot_path, baseline=False)
  path_to_read = (
      client_path
      if os.path.exists(client_path)
      else resources.GetResourceFilename(depot_path.removeprefix("//depot/"))
  )
  with open(path_to_read, "r", encoding="utf-8") as f:
    return f.read()


def validate_component_catalog_sync() -> (
    presubmit_stubby_service_pb2.PresubmitResponse
):
  """Validates that every gBreeze catalog component is documented in skills."""
  build_content = _read_file_content(_LABS_GB_BUILD_DEPOT_PATH)
  component_dirs = sorted(set(_CATALOG_ENTRY_RE.findall(build_content)))
  if not component_dirs:
    return presubmit_stubby_service_pb2.PresubmitResponse(
        succeeded=False,
        failure_message=(
            f"Expected to find catalog_group entries in"
            f" {_LABS_GB_BUILD_DEPOT_PATH}."
        ),
    )

  skill_contents = {
      depot_path.removeprefix("//depot/"): _read_file_content(depot_path)
      for depot_path in _SKILL_DEPOT_PATHS
  }

  missing_by_component = {}
  for comp_dir in component_dirs:
    tag_name = _COMPONENT_DIR_TO_TAG.get(comp_dir, f"md-gb-{comp_dir}")
    missing_files = [
        rel_path
        for rel_path, content in skill_contents.items()
        if tag_name not in content
    ]
    if missing_files:
      missing_by_component[f"{comp_dir} (<{tag_name}>)"] = missing_files

  if missing_by_component:
    details = "\n".join(
        f"  - {comp}: missing in {', '.join(files)}"
        for comp, files in missing_by_component.items()
    )
    return presubmit_stubby_service_pb2.PresubmitResponse(
        succeeded=False,
        failure_message=(
            "New gBreeze components registered in"
            " //third_party/javascript/material/web/labs/gb/BUILD must be"
            f" added to the GenUX and gBreeze skill files:\n{details}"
        ),
    )

  return presubmit_stubby_service_pb2.PresubmitResponse(succeeded=True)


def main(argv: Sequence[str]) -> None:
  if len(argv) > 1:
    raise app.UsageError("Too many command-line arguments.")

  response = validate_component_catalog_sync()
  try:
    presubmit_utils.WriteResponse(response)
  except Exception as e:  # pylint: disable=broad-exception-caught
    sys.stderr.write(f"Failed to write presubmit response: {e}\n")
  if not response.succeeded:
    sys.stderr.write(f"{response.failure_message}\n")
    sys.exit(1)


if __name__ == "__main__":
  app.run(main)
