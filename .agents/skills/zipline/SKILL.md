---
name: zipline
description: >-
  Share websites, dashboards, prototypes, and HTML reports via Zipline
  (go/zipline) using the UX Design Intelligence upload tool. Manages uploading
  directories or ZIP archives to new or existing assets. Use when sharing a
  generated report or dashboard, uploading a static web app build, or pushing
  prototype updates to Zipline.
---

# Zipline Upload Skill

This skill allows Gemini to interact with Zipline (Google's internal static file
hosting service) to upload content such as HTML websites, built distribution
directories, or ZIP archives using the UX Design Intelligence transfer tool.

## Implementation

The tool is implemented as a Python binary and library:

*   **Binary Target:**
    `//ux/design_intelligence/crowdcompute/transfer/zipline:zipline_upload`
*   **Python Source:**
    `google3/ux/design_intelligence/crowdcompute/transfer/zipline/zipline_upload.py`
*   **Library API:**
    `google3/ux/design_intelligence/crowdcompute/transfer/zipline/zipline_upload_lib.py`

--------------------------------------------------------------------------------

## 🛠️ Tool Usage Instructions (CLI Mode)

Always use the `run_command` tool to execute the Zipline uploader via `blaze
run`:

```bash
blaze run //ux/design_intelligence/crowdcompute/transfer/zipline:zipline_upload -- [flags]
```

### Available Flags

1.  `--source` (Optional if creating a project): Path to the source directory
    (e.g., `dist`, `$PWD/dist`, `/google/src/cloud/user/ws/google3/app`) or
    `.zip` archive to upload. Relative paths are supported and will be resolved
    correctly even when running via `blaze run`.
2.  `--project_id` (Optional): The Zipline Project ID (if uploading to an
    existing project). If omitted, `--project_name` must be specified.
3.  `--project_name` (Optional): Display name of a new Zipline project to create
    if `--project_id` is not provided. Can be used without `--source` to simply
    create a new project. Spaces are allowed and will be automatically replaced
    with underscores (e.g., `My New Project` becomes `My_New_Project`).
4.  `--display_name` (Required when uploading): Human-readable title for the
    asset. Spaces are allowed and will be automatically replaced with
    underscores (max 80 chars; e.g., `Live Demo Asset` becomes
    `Live_Demo_Asset`).
5.  `--asset_id` (Optional): Existing Asset ID. If omitted, a brand new asset is
    created inside the target project.
6.  `--description` (Optional): Description of this specific upload version
    snapshot or project.

### CLI Examples

**Creating a new project and uploading an asset:**

```bash
blaze run //ux/design_intelligence/crowdcompute/transfer/zipline:zipline_upload -- \
  --source="$PWD/dist" \
  --project_name="My_New_Prototype_Project" \
  --display_name="Live_Demo_Asset" \
  --description="Initial prototype build"
```

**Creating a new asset in an existing project:**

```bash
blaze run //ux/design_intelligence/crowdcompute/transfer/zipline:zipline_upload -- \
  --source="$PWD/dist" \
  --project_id="12345678-abcd-1234-abcd-1234567890ab" \
  --display_name="My_Prototype_Live_Demo" \
  --description="Initial prototype build"
```

**Updating an existing asset with a new version:**

```bash
blaze run //ux/design_intelligence/crowdcompute/transfer/zipline:zipline_upload -- \
  --source="$PWD/dist" \
  --project_id="12345678-abcd-1234-abcd-1234567890ab" \
  --asset_id="87654321-dcba-4321-dcba-0987654321ba" \
  --display_name="My_Prototype_Live_Demo" \
  --description="Updated AI Mode layout split screen"
```

**Creating a project standalone without uploading files:**

```bash
blaze run //ux/design_intelligence/crowdcompute/transfer/zipline:zipline_upload -- \
  --project_name="My_New_Zipline_Project" \
  --description="Project created via CLI"
```

--------------------------------------------------------------------------------

## 🐍 Programmatic Usage Instructions (Python Library)

When authoring custom Python automation or deployment scripts, import
`zipline_upload_lib` directly:

```python
import pathlib
from google3.ux.design_intelligence.crowdcompute.transfer.zipline import zipline_upload_lib

# Creating a project standalone
project_id = zipline_upload_lib.create_project("My_New_Project", description="Created programmatically")

# Or creating a project automatically during upload by specifying project_name instead of project_id
def deploy_to_zipline(source_dir: str, display_name: str, project_id: str = None, project_name: str = None, asset_id: str = None):
    source_path = pathlib.Path(source_dir).resolve()

    asset = zipline_upload_lib.upload_to_zipline(
        source=source_path,
        project_id=project_id,
        project_name=project_name,
        display_name=display_name,
        asset_id=asset_id,
        description="Automated Python library deployment"
    )

    print(f"🚀 Root Serving URL: {zipline_upload_lib.zipline_asset_root_url(asset.asset_id)}")
    print(f"📌 Version Snapshot URL: {zipline_upload_lib.zipline_asset_versioned_url(asset.version_id)}")
    return asset
```

**Required `BUILD` dependency:**

```python
deps = [
    "//ux/design_intelligence/crowdcompute/transfer/zipline:zipline_upload_lib",
]
```

--------------------------------------------------------------------------------

## 🔒 Guidelines & Best Practices

1.  **Compile Before Upload:** If uploading a compiled web application (such as
    React, Vite, or TypeScript prototypes), you **MUST** ensure the production
    build is compiled (e.g., by running `npm run build` in the prototype root
    directory) to generate the static distribution folder (typically `dist/` or
    `build/`) before executing the uploader.

2.  **Authentication (Uplink):** Zipline operations route through the Üplink
    proxy. If an upload fails with proxy authentication errors, instruct the
    user to run:

    ```bash
    uplink-helper login -noglogin
    ```

3.  **Project IDs vs Names:** If the user does not have an existing Project ID
    from [go/zipline](http://go/zipline), you can use `--project_name` to
    automatically create a new project during upload or as a standalone step.

    > [!IMPORTANT] **CRITICAL GOTCHA:** If the user has never logged into the
    > Zipline Web UI (`go/zipline`), creating a project will fail with a backend
    > database error (`violates foreign key constraint
    > "project_users_user_id_fkey"`). You **MUST** ask the user to visit
    > http://go/zipline in their browser to initialize their account before
    > attempting to create a project via CLI or Python library.

4.  **Naming Restrictions (Spaces Auto-Sanitized):** Both Zipline project names
    (`--project_name`) and asset display names (`--display_name`) strictly
    forbid spaces at the API level. However, the uploader tool automatically
    sanitizes names by replacing spaces with underscores (e.g., `My New Project`
    ➔ `My_New_Project`). You can safely pass names with spaces.

5.  **Relative Source Paths Supported:** Historically, `blaze run` required
    absolute paths because it executes inside a sandbox. The uploader tool now
    detects the `BUILD_WORKING_DIRECTORY` environment variable and automatically
    resolves relative paths (e.g. `--source="dist"`) against the directory where
    you executed the command.

6.  **Resilient Execution:** Asset creation and file uploading can take a minute
    or two for large builds. Do not abort terminal commands early.

7.  **URL Presentation:** When the command completes, always extract and display
    the internal serving URLs to the user clearly:

    *   **Live Serving URL:**
        `https://serve-dot-zipline.googleplex.com/asset/<ASSET_ID>/`
    *   **Version Snapshot URL:**
        `https://serve-dot-zipline.googleplex.com/hosting/<VERSION_ID>`
