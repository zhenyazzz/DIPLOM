import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowUpRight, LoaderCircle } from "lucide-react";
import { motion } from "motion/react";
import { login, register } from "../api/identity/auth";
import { ApiError } from "../api/http";
import { AuthScene } from "../components/auth/AuthScene";
import { Button } from "../components/ui/Button";
import { Field, Input } from "../components/ui/Input";
import { fadeUp, scaleIn, staggerContainer } from "../lib/motion";

export function RegisterPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const displayName = String(form.get("displayName") ?? "").trim();
    const password = String(form.get("password") ?? "");
    setLoading(true);
    setError(null);
    try {
      await register({ email, displayName, password });
      await login({ email, password });
      navigate("/scenario");
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.message
          : "Не удалось зарегистрироваться.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthScene
      title="Регистрация команды"
      subtitle="Создайте аккаунт и откройте реестр проектов и оптимизацию портфеля."
    >
      <motion.form
        onSubmit={onSubmit}
        variants={scaleIn}
        initial="hidden"
        animate="show"
        className="surface shadow-card w-full max-w-md justify-self-end bg-panel/95 p-7 backdrop-blur-md sm:p-8"
      >
        <p className="text-[12px] font-medium text-mute">Access</p>
        <h2 className="mt-2 font-display text-[1.75rem] text-paper">Регистрация</h2>
        <p className="mt-2 text-[14px] text-mute">
          Укажите имя, email и пароль — после создания вы сразу войдёте в систему.
        </p>

        <motion.div
          className="mt-6 grid gap-4"
          variants={staggerContainer}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={fadeUp}>
            <Field label="Имя" htmlFor="displayName">
              <Input
                id="displayName"
                name="displayName"
                type="text"
                placeholder="Анна Иванова"
                required
              />
            </Field>
          </motion.div>
          <motion.div variants={fadeUp}>
            <Field label="Email" htmlFor="email">
              <Input id="email" name="email" type="email" placeholder="you@company.local" required />
            </Field>
          </motion.div>
          <motion.div variants={fadeUp}>
            <Field label="Пароль" htmlFor="password">
              <Input
                id="password"
                name="password"
                type="password"
                minLength={8}
                placeholder="минимум 8 символов"
                autoComplete="new-password"
                required
              />
            </Field>
          </motion.div>
        </motion.div>

        {error && (
          <p className="mt-4 rounded-md bg-reject/10 px-3 py-2 text-[13px] text-reject">
            {error}
          </p>
        )}

        <Button
          type="submit"
          className="mt-6 w-full"
          disabled={loading}
          data-cursor-label="Создать"
        >
          {loading ? (
            <>
              <LoaderCircle className="animate-spin" size={14} />
              Создание…
            </>
          ) : (
            <>
              Создать аккаунт
              <ArrowUpRight size={14} />
            </>
          )}
        </Button>

        <p className="mt-5 text-center text-[13px] text-mute">
          Уже есть аккаунт?{" "}
          <Link to="/login" className="text-paper underline-offset-2 hover:text-accent hover:underline">
            Войти
          </Link>
        </p>
      </motion.form>
    </AuthScene>
  );
}
