load("//learning/gemini/agents/skill_rules:skills.bzl", "agent_skill")
load("//learning/gemini/agents/skills/skill_creator/scripts:skill_defs.bzl", "validate_skill_test")

package(default_visibility = [
    "//labs/ix:__subpackages__",
    "//learning/gemini/agents/skills:__subpackages__",
])

exports_files([
    "EVAL.txtpb",
    "SKILL.md",
])

# Payload boundary. A skill's runtime contract is SKILL.md plus references/*.md.
# Specs, design docs, READMEs and user guides are written for people; an agent
# that browses this directory and reads one acts on stale or aspirational
# statements. Enforcing it at loading time costs zero runtime tokens and cannot
# be ignored, unlike a prose prohibition in SKILL.md.
#
# This is the same invariant as validate_skill_payload() in
# //labs/ix/gthink:skill_payload.bzl, inlined rather than loaded: the shared
# catalog copy of this skill deliberately depends on nothing under
# //labs/ix/gthink except the analyzer scripts wired into agent_skill below.
[
    fail(
        ("Non-payload Markdown in skill directory: %s. The runtime payload is " +
         "SKILL.md plus references/*.md. Move specs, design docs, READMEs and " +
         "user guides to //labs/ix/gthink/docs, outside the directory the " +
         "agent browses. If the content IS needed at runtime, put it in " +
         "references/ and cite it from SKILL.md.") % stray,
    )
    for stray in glob(["*.md"])
    if stray != "SKILL.md"
]

validate_skill_test(
    name = "validate_gthink_skill_test",
)

test_suite(
    name = "validate_skill_test",
    tests = [":validate_gthink_skill_test"],
)

agent_skill(
    name = "gthink",
    srcs = [
        "SKILL.md",
    ] + glob(["references/*.md"]),
    scripts = [
        "//labs/ix/gthink/skills/gthink/scripts:analyze_tasks.py",
        "//labs/ix/gthink/skills/gthink/scripts:analyze_emails.py",
        "//labs/ix/gthink/skills/gthink/scripts:analyze_transcripts.py",
        "//labs/ix/gthink/skills/gthink/scripts:analyze_cls.py",
        "//labs/ix/gthink/skills/gthink/scripts:analyze_hr_profile.py",
        "//labs/ix/gthink/skills/gthink/scripts:retrieve_content.py",
        "//labs/ix/gthink/skills/gthink/scripts:survey_cls.py",
        "//labs/ix/gthink/skills/gthink/scripts:verify_grounding.py",
    ],
    visibility = ["//visibility:public"],
)
