# Assistant handoff instructions

Before changing product behavior, read [PROJECT_DIRECTION.md](PROJECT_DIRECTION.md). It records Lillyhop's user-approved direction, known implementation state, and workflow safeguards.

Keep that document current when Cathy makes an important product decision or verified implementation status changes. Separate user decisions from suggestions and unverified assumptions. Do not silently change direction. Ask before consequential changes to tracking, monetization, data sharing, or product scope.

Inspect `git status` before and after work. Preserve unrelated local edits and stage only files in scope. Never edit, stage, commit, revert, or push `app/src/main/java/com/pedometer/app/domain/SensorFoldLogic.kt` without Cathy's explicit permission; she previously asked to leave its local edit alone.

Do not make unsupported accuracy, health, privacy, or security promises. Run the narrowest applicable validation and report blockers clearly.
