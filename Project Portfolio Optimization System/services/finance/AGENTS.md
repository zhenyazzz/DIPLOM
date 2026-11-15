# finance service

Port **8082**. Schema `finance`.

Import sessions, snapshots, cash flows, parse and NPV; owns financial facts.  
Calls FTS for file bytes; afterCommit → notify on publish/fail.

Scaffold — business logic in W4. Канон: `01_ARCHITECTURE_Portfolio_Optimizer.md`.
