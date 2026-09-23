# Security policy

## Supported versions

| Version | Supported |
|---------|-----------|
| Latest on `master` | Yes |
| Older tags / forks | Best effort |

## Reporting a vulnerability

If you believe you have found a security issue in pen:

1. **Do not** open a public GitHub issue with exploit details.
2. Open a **private** report via [GitHub Security Advisories](https://github.com/altenhofen/pen/security/advisories/new) for this repository, or contact the maintainer through GitHub (@altenhofen) with a clear description and steps to reproduce.

Please include:

- Affected version or commit
- Impact (e.g. data exposure, privilege escalation)
- Proof of concept if available

We will acknowledge receipt as soon as possible and work on a fix before public disclosure when appropriate.

## Data handling note

pen stores handwriting prototypes locally on the device. It does not upload your strokes to a backend in the current prototype; still, treat on-device data as sensitive when testing on shared devices.
