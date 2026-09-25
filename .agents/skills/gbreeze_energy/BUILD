load("//learning/gemini/agents/skill_rules:skills.bzl", "agent_skill")
load("//learning/gemini/agents/skills/skill_creator/scripts:skill_defs.bzl", "validate_skill_test")

package(default_visibility = ["//visibility:public"])

agent_skill(
    name = "gbreeze-energy",
    srcs = [
        "SKILL.md",
    ] + glob(["references/**/*.md"]),
)

validate_skill_test(
    name = "validate_gbreeze_energy_test",
    agent_skill = ":gbreeze-energy",
)
