# Security Policy

BerongSMP is a university thesis project: a Minecraft mod that runs a disaster preparedness
training simulation and reports what happens during a session to a Turso cloud database for a
companion machine learning pipeline. It is not a production service with an uptime commitment, but
it does handle real student accounts (usernames and passwords) and a live database connection, so
security reports are still taken seriously.

## Supported versions

This project does not maintain multiple release branches. Only the code on `main` is supported.
There is no backport policy for older commits.

## What is in scope

- Authentication and password handling (`AuthManager`, `PasswordHasher`, and the `/register` and
  `/login` commands)
- Handling of the Turso database URL and access token (`TursoClient`, `berongsmp-common.toml`)
- Command permission checks, in particular anything gated behind OP level or `/bfp login`
  (`BfpAdminCommands`, and the permission checks in `RegistrationCommands`/`SimulationCommands`)
- Network payload handling between the mod client and server (`network/` package)

## What is out of scope

- The Minecraft server itself, NeoForge, or any third party mod or library this project depends on.
  Report those upstream, to the NeoForge project or the relevant maintainer.
- Denial of service against a single classroom deployment. This mod is designed to run for a
  bounded group of students on hardware the instructor controls, not as internet-facing
  infrastructure.
- Anything that requires physical or admin access to the machine the server or database already
  runs on.

## Reporting a vulnerability

Please do not open a public GitHub issue for a security problem. Instead, use GitHub's private
vulnerability reporting feature on this repository (the "Security" tab, then "Report a
vulnerability"). This lets you describe the issue privately before it is visible to anyone else.

If you are not able to use that feature, opening a regular issue that simply says you have found a
security concern and asks for a private way to reach out is fine, please just avoid posting
exploit details in public.

When reporting, it helps to include:

- A clear description of the issue and why it matters
- Steps to reproduce it, or a proof of concept if you have one
- The affected file or command, if you know it

There is no bug bounty for this project since it is unpaid coursework, but genuine reports are
appreciated and will be credited in the fix commit unless you would rather stay anonymous.
