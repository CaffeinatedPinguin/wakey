# wakey

> Open source uptime monitor with HTTP, TCP, ping and heartbeat checks, agents in private networks, alerts, incidents and public status pages. Self-host without external services, or use the EU cloud.

[![ci](https://github.com/CaffeinatedPinguin/wakey/actions/workflows/ci.yml/badge.svg)](https://github.com/CaffeinatedPinguin/wakey/actions/workflows/ci.yml)
[![version](https://img.shields.io/github/v/release/CaffeinatedPinguin/wakey?label=version&sort=semver)](https://github.com/CaffeinatedPinguin/wakey/releases/latest)
[![license](https://img.shields.io/github/license/CaffeinatedPinguin/wakey)](LICENSE)

**Status:** before stage 0: only documentation and decisions exist, with no code and no release. Maintained by one person, best effort. See [Support](#support).

## Features

- Planned: HTTP and TCP checks from the server with assertions, timeouts, retries and a slow-response threshold.
- Planned: certificate expiry checks and heartbeat checks for cron jobs and scheduled tasks.
- Planned: agents that run checks inside private networks, limited to targets on a local allow-list.
- Planned: alerts over email, webhook and Pushover, with acknowledgment, snooze and bulk muting.
- Planned: automatic and manual incidents, planned maintenance windows and post-mortems.
- Planned: public status pages with components, history bars and incident updates.
- Planned: spaces, roles, API tokens, an audit log and a declarative configuration file.
- Planned: self-host as a Docker image with compose, or use the hosted EU cloud.

## Installation

There is no release yet. A Docker image and a `docker compose` setup are planned from stage 2 of the [roadmap](docs/specs/roadmap.md).

## Usage

From stage 1 of the [roadmap](docs/specs/roadmap.md) you can see the state of your own checks in the panel and through the API; the first alert on your phone arrives with stage 2.

## Documentation

Start at [docs/index.md](docs/index.md) and check [docs/specs/roadmap.md](docs/specs/roadmap.md) for the stage order. The documentation is in Polish for now; an English translation is planned.

## Support

Bug reports and feature requests: [open an issue](https://github.com/CaffeinatedPinguin/wakey/issues/new/choose) using a form. Pull requests from outside contributors are not accepted. Contributing: [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

Report vulnerabilities privately. See [SECURITY.md](SECURITY.md). Do not open public issues for security problems.

## License

[AGPL-3.0-or-later](LICENSE) © CaffeinatedPinguin
