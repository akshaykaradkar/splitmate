# gBreeze in google3

## Overview

Google3 requires compiling gBreeze components via Bazel instead of using a CDN.

## 1. Import components

Import components into a TypeScript entrypoint (e.g., `app.ts`):

```ts
import m3Stylesheet from '@material/web/labs/gb/styles/m3.cssresult';
import '@material/web/labs/gb/components/button/md-button';
import '@material/web/labs/gb/components/card/md-card';
import '@material/web/labs/gb/styles/icon/md-icon';
```

## 2. Update BUILD deps

Use go/build-cleaner to update build dependencies.

```BUILD
ts_library(
    name = "app_ts",
    srcs = ["app.ts"],
    deps = [
        "//third_party/javascript/material/web/labs/gb/components/button:ts",
        "//third_party/javascript/material/web/labs/gb/components/card:ts",
        "//third_party/javascript/material/web/labs/gb/styles:ts",
        "//third_party/javascript/material/web/labs/gb/styles/icon:ts",
    ],
)
```