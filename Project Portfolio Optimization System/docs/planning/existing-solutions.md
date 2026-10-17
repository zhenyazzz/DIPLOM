# Анализ существующих решений (аналоги)

**Связь с заданием:** REQ-PRE-02 (≥2–3 аналога), DOC-412.  
**Дата обращения к источникам:** 17.10.2026.  
**Правило:** функции только с URL; иначе — «не подтверждено».  
**Статус выводов:** `proposed` (для ПЗ); не утверждать превосходство учебного прототипа над коммерцией.

---

## 1. Логика выбора аналогов

Рынок делится на:
- **PPM / SPM** — реестр, финансы, сценарии, scoring (часто без публично подтверждённой классической МКА);
- **DSS / MCDA** — AHP/PAPRIKA, чувствительность, иногда funding constraints (слабее как полный PM).

Для темы курсовой релевантна **комбинация** обоих классов.

**Рекомендуемый минимум для §4.1.2 `[Р]`:**
1. TransparentChoice (DSS + портфельные ограничения)  
2. Expert Choice Comparion (AHP) **или** Super Decisions (учебный AHP)  
3. Oracle Primavera Cloud Portfolio **или** ServiceNow SPM **или** Microsoft Project Online Portfolio Analysis (enterprise PPM + финансы/сценарии)  
Опционально 4-й: 1С:PM / ADVANTA (региональный контекст).

---

## 2. Краткие карточки (подтверждённое)

### TransparentChoice — DSS
- AHP, pairwise, consistency, sensitivity — [ПОДТВЕРЖДЕНО] https://www.transparentchoice.com/analytic-hierarchy-process · https://www.transparentchoice.com/software  
- Портфельные решения с funding/capacity constraints — [ПОДТВЕРЖДЕНО] https://www.transparentchoice.com/software/project-prioritization  
- Полноценный PM (Гантт, учёт факта) — [НЕ ПОДТВЕРЖДЕНО]

### Expert Choice Comparion — DSS
- AHP, what-if/sensitivity, resource allocation — [ПОДТВЕРЖДЕНО] https://www.expertchoice.com/comparion  
- Capital budgeting / project prioritization как область применения — [ПОДТВЕРЖДЕНО] (заявления вендора на той же странице)  
- Реестр исполнения как в PPM — [НЕ ПОДТВЕРЖДЕНО]

### 1000minds / Super Decisions
- 1000minds: PAPRIKA, value for money — [ПОДТВЕРЖДЕНО] https://www.1000minds.com/paprika  
- Super Decisions: бесплатный AHP/ANP — [ПОДТВЕРЖДЕНО] https://www.superdecisions.com/downloads/

### Oracle Primavera Cloud — Portfolio & Capital Planning
- Scoring, weights, evaluation matrix — [ПОДТВЕРЖДЕНО] https://www.oracle.com/construction-engineering/oracle-primavera-cloud-portfolio-and-capital-planning/  
- What-if, waterline, efficient frontier; budget/funding — [ПОДТВЕРЖДЕНО] там же  
- Pairwise AHP — [НЕ ПОДТВЕРЖДЕНО]

### ServiceNow SPM
- Portfolio/Financial planning; scoring (RICE, WSJF, custom); scenarios — [ПОДТВЕРЖДЕНО] https://www.servicenow.com/products/strategic-portfolio-management.html и docs по scenario/scoring  
- Классический AHP — [НЕ ПОДТВЕРЖДЕНО]

### Microsoft Project Online Portfolio Analysis / Planner Portfolios
- Business drivers, cost/resource optimization, efficient frontier — [ПОДТВЕРЖДЕНО] https://learn.microsoft.com/en-us/projectonline/portfolio-analysis-overview · perform-cost-optimization  
- Planner Portfolios — мониторинг планов — [ПОДТВЕРЖДЕНО] support.microsoft.com (Planner portfolios)  
- Единый продукт Planner = Project Online Analysis — [НЕ ПОДТВЕРЖДЕНО]  
- Формальная МКА AHP в Planner — [НЕ ПОДТВЕРЖДЕНО]

### Planview / Broadcom Clarity
- Prioritization, financials, scenario/what-if — [ПОДТВЕРЖДЕНО] planview.com (PPM), docs.broadcom.com (Clarity portfolio)  
- Конкретный метод AHP/PAPRIKA — [НЕ ПОДТВЕРЖДЕНО]

### Региональные / OSS
- **1С:PM** — портфели, план-факт, приоритеты — [ПОДТВЕРЖДЕНО] https://solutions.1c.ru/catalog/pm-prof/features · МКА — [НЕ ПОДТВЕРЖДЕНО]  
- **ADVANTA** — портфель, бюджет/ROI, рейтинг — [ПОДТВЕРЖДЕНО] https://www.advanta-group.ru/solutions/upravlenie-portfelem-proektov/ · алгоритм МКА — [НЕ ПОДТВЕРЖДЕНО]  
- **OpenProject Portfolios** — группировка/статус — [ПОДТВЕРЖДЕНО] https://www.openproject.org/docs/user-guide/portfolios/ · оптимизация/МКА — [НЕ ПОДТВЕРЖДЕНО]  
- **Spider Project** — мультипроект, финансы, риски — [ПОДТВЕРЖДЕНО] https://www.spiderproject.ru/ru/sp-prof/ · формальная МКА отбора — [НЕ ПОДТВЕРЖДЕНО]

---

## 3. Сравнительная таблица (ядро)

| Критерий | TransparentChoice | Comparion | Oracle Portfolio | ServiceNow SPM | Project Online PA | 1С:PM |
|---|---|---|---|---|---|---|
| Реестр проектов | ~ | ~ | + | + | + | + |
| Критерии/веса / scoring | + AHP | + AHP | + | + frameworks | + drivers | ~ |
| Формальная МКА | + | + | − | − | − | − |
| Бюджет / финансы портфеля | ~ constraints | ~ | + | + | + | + |
| Сценарии / оптимизация набора | + | + | + frontier | + | + frontier | −/~ |
| Исполнение PM | − | − | + (платформа) | + | + | + |

`+` подтверждено URL · `~` частично · `−` не подтверждено.

---

## 4. Пробелы, оправдывающие учебную разработку `[ВЫВОД]`

Не «лучше коммерции», а **учебный сквозной контур**, обычно разнесённый по продуктам:

1. Карточка проекта → критерии МКА → балл → отбор при бюджете → сценарии — в одном приложении.  
2. Прозрачная методика (формулы, нормализация, веса, объяснение невключения).  
3. Импорт финансовых данных с валидацией (учебный масштаб).  
4. Аудит решения (кто, какие веса, почему вошёл/не вошёл).  
5. Без enterprise-ресурсов, Monte Carlo, полной ERP.

---

## 5. Требования к разрабатываемой системе (из аналогов) → трассировка

| Выведенное требование | Источник идеи | Связь с заданием |
|---|---|---|
| Реестр заявок/проектов | PPM-линейка | REQ-MIN-05 |
| Критерии и веса / МКА | TransparentChoice, Comparion | REQ-MIN-07, REQ-AN-01 |
| Финансовые показатели и бюджетные ограничения | Oracle, ServiceNow, 1С | тема + REQ-MIN-08 |
| Сценарии «что если» (2+) | Oracle, MS, Planview, TransparentChoice | предметные ФТ |
| Отчёт для ЛПР | все | REQ-MIN-08 |
| ≥3 роли (аналитик / ЛПР / …) | практика PPM + REQ-MIN-04 | REQ-MIN-04 |

---

## 6. Список ключевых URL

См. полный перечень в отчёте агента F (дата 17.10.2026). Минимальный набор для ПЗ: TransparentChoice software/AHP; Expert Choice Comparion; Oracle Primavera portfolio page; ServiceNow SPM; Microsoft Learn portfolio-analysis-overview; 1С:PM features; Super Decisions downloads.
