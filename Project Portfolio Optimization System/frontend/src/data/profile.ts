export type ProfileActivity = {
  id: string;
  time: string;
  title: string;
  detail: string;
  tone: "accent" | "signal" | "amber" | "mute";
};

export type ProfileCompetence = {
  id: string;
  label: string;
  level: number;
  hint: string;
};

export const currentUser = {
  id: "USR-1042",
  firstName: "Анна",
  lastName: "Иванова",
  shortName: "Иванова А.",
  initials: "АИ",
  title: "Expert · Portfolio Analyst",
  role: "Expert",
  department: "Инвестиционный комитет",
  organization: "Enterprise Capital",
  email: "a.ivanova@enterprise.local",
  phone: "+7 (495) 120-44-18",
  location: "Москва · UTC+3",
  joined: "март 2023",
  bio: "Ведёт цикл отбора инвестиционных проектов: нормализация SAW, бюджетные ограничения и проверка зависимостей. Фокус — прозрачные решения и воспроизводимые сценарии.",
  access: ["Реестр проектов", "Оптимизация", "Утверждение сценариев", "Экспорт отчётов"],
  stats: [
    { label: "Сценарии", value: "47", hint: "за 12 мес." },
    { label: "Утверждено", value: "18", hint: "портфелей" },
    { label: "Средний SAW", value: "0.81", hint: "отбор" },
    { label: "Бюджет", value: "₽2.4 млрд", hint: "под контролем" },
  ],
  competences: [
    {
      id: "saw",
      label: "SAW / мультикритерии",
      level: 0.92,
      hint: "Веса, нормализация, чувствительность",
    },
    {
      id: "knapsack",
      label: "Knapsack 0-1",
      level: 0.86,
      hint: "Бюджетные ограничения и плотность ценности",
    },
    {
      id: "deps",
      label: "Граф зависимостей",
      level: 0.78,
      hint: "Валидация комбинаций и конфликты",
    },
    {
      id: "governance",
      label: "Governance",
      level: 0.84,
      hint: "Протоколы ИК и трассируемость",
    },
  ] satisfies ProfileCompetence[],
  activity: [
    {
      id: "a1",
      time: "Сегодня · 14:22",
      title: "Сценарий Q4 зафиксирован",
      detail: "Отобрано 9 проектов · SAW Σ 7.42 · бюджет 94%",
      tone: "accent",
    },
    {
      id: "a2",
      time: "Вчера · 18:05",
      title: "Зависимость PRJ-014 → PRJ-008",
      detail: "Конфликт устранён: исключён кандидат с низким NPV",
      tone: "amber",
    },
    {
      id: "a3",
      time: "12 ноя · 11:40",
      title: "Импорт реестра",
      detail: "24 кандидата из Investment Hub · статус SCORED",
      tone: "signal",
    },
    {
      id: "a4",
      time: "08 ноя · 09:15",
      title: "Отчёт для ИК",
      detail: "Экспорт PDF: сравнение baseline vs. stress −10% NPV",
      tone: "mute",
    },
  ] satisfies ProfileActivity[],
  preferences: {
    theme: "Системная",
    locale: "ru-RU",
    notifications: "Этапы сценария · утверждения",
    defaultView: "Пространственный реестр",
  },
};
