# Documentation

Two copies of the same documents, one per language. Each file in one folder has a counterpart with the same name in the other.

| Folder | Language | Role |
| --- | --- | --- |
| [en/](en/) | English | The original. Code comments, commit messages and the other repositories (`onward-gateway`, `onward-notification`) link here. |
| [vi/](vi/) | Tiếng Việt | Bản dịch. Khi một tài liệu trong `en/` thay đổi thì bản `vi/` tương ứng được cập nhật theo trong cùng một pull request. |

Documents:

| File | What it covers |
| --- | --- |
| `authentication-authorization.md` | Identity, tenant and role design: registration, login, sessions, CSRF, the role matrix, tenant isolation, API contracts |
| `authentication-implementation.md` | How the authentication design is implemented in the code |
| `data-and-domain-model.md` | Domain classes, JPA mappings and tables for users, organizations, memberships, sessions and tokens |
| `registration-api.md` | The registration endpoint and the RSA key pair it needs at startup |
| `microservice-decomposition.md` | The plan and the decisions for moving from the modular monolith to services: Notification (Go), JWKS, gateway, denylist |
| `incident-service.md` | The plan for the Incident service, the first business service outside Account |
