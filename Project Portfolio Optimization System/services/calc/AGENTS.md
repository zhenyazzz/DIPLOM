# calc service

Port **8081**. **Без БД.**

Stateless HTTP: SAW (`/v1/scoring/saw`) и 0-1 оптимизация (`/v1/optimization/binary`).  
Только docker-сеть / internal — не на gateway.

Вызывается из `domain`. Канон: `01_ARCHITECTURE_Portfolio_Optimizer.md` (ARCH-04, ARCH-10).
