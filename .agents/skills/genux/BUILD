load("//devtools/python/blaze:pytype.bzl", "pytype_strict_contrib_test")
load("//learning/gemini/agents/skill_rules:skills.bzl", "agent_skill")
load("//learning/gemini/agents/skills/skill_creator/scripts:skill_defs.bzl", "validate_skill_test")

package(default_visibility = ["//visibility:public"])

# Eval commands:
#
# evalin run ux/gdp/ai/plugins/material_genux_kit/skills/genux/EVAL.txtpb --runs=3
# evalin run ux/gdp/ai/plugins/material_genux_kit/skills/genux/EVAL.txtpb --runs=3 --with-vs-without-skills
# (NOT ux/gdp/ai/skills/genux/evals/EVAL.txtpb — that is the gbreeze suite and it publishes to a prod dashboard.)

agent_skill(
    name = "genux",
    srcs = [
        "SKILL.md",
        "references/design.md",
        "references/genui.md",
    ] + glob(["assets/**"]),
)

validate_skill_test(
    name = "validate_genux_test",
    agent_skill = ":genux",
)

pytype_strict_contrib_test(
    name = "component_catalog_sync_test",
    srcs = ["component_catalog_sync_test.py"],
    data = [
        "SKILL.md",
        "assets/starter_template.html",
        "references/genui.md",
        "//devtools/api/source/presubmit_build_target",
        "//third_party/javascript/material/web/labs/gb:BUILD",
        "//ux/gdp/ai/skills/gbreeze:SKILL.md",
    ],
    deps = [
        "//devtools/api/source:presubmit_stubby_service_py_pb2",
        "//devtools/api/source/presubmit_build_target/python:presubmit_utils",
        "//pyglib:resources",
        "//third_party/py/absl:app",
    ],
)
