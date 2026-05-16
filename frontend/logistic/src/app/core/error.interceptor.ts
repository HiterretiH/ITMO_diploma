import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import {
  localizeHttpClientMessage,
  localizeProblemToast,
  summaryAndDetailForPlainHttpError,
} from './error-messages';
import { ProblemDetail } from '../models/problem.models';

function parseProblem(err: HttpErrorResponse): { summary: string; detail?: string } {
  const body = err.error;
  if (body && typeof body === 'object' && !Array.isArray(body)) {
    const p = body as ProblemDetail;
    const toast = localizeProblemToast(p.title, p.detail, err.status);
    return {
      summary: toast.summary,
      detail: toast.detail,
    };
  }
  return {
    summary: localizeProblemToast(null, null, err.status).summary,
    detail: localizeHttpClientMessage(err.message, err.status),
  };
}

/** Suppress toast for optimistic-lock style conflicts on order update */
function isOrderPut(req: { method: string; url: string }): boolean {
  return (
    req.method === 'PUT' &&
    /\/api\/v1\/orders\/[^/]+\/?$/.test(req.url.split('?')[0] ?? '')
  );
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const messages = inject(MessageService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const ct = err.headers?.get('content-type') ?? '';
      const isProblem =
        ct.includes('application/problem+json') ||
        ct.includes('application/json');

      if (err.status === 401) {
        const loginUrl = '/api/v1/auth/login';
        const registerUrl = '/api/v1/auth/register';
        if (req.url.includes(loginUrl) || req.url.includes(registerUrl)) {
          const defaultDetail = req.url.includes(registerUrl)
            ? 'Не удалось зарегистрироваться'
            : 'Неверный логин или пароль';
          if (isProblem && err.error && typeof err.error === 'object') {
            const { summary, detail } = parseProblem(err);
            messages.add({
              severity: 'error',
              summary,
              detail: detail ?? defaultDetail,
              life: 6000,
            });
          } else {
            messages.add({
              severity: 'error',
              summary: req.url.includes(registerUrl)
                ? 'Ошибка регистрации'
                : 'Ошибка входа',
              detail: defaultDetail,
              life: 6000,
            });
          }
        } else if (auth.isLoggedIn()) {
          auth.logout();
        }
        return throwError(() => err);
      }

      if (err.status === 403) {
        if (req.url.includes('/api/v1/auth/login')) {
          return throwError(() => err);
        }
        void router.navigateByUrl('/forbidden');
        if (isProblem && err.error && typeof err.error === 'object') {
          const { summary, detail } = parseProblem(err);
          messages.add({
            severity: 'warn',
            summary,
            detail,
            life: 5000,
          });
        }
        return throwError(() => err);
      }

      if (err.status >= 500) {
        const parsed = parseProblem(err);
        messages.add({
          severity: 'error',
          summary: parsed.summary || 'Ошибка сервера',
          detail:
            parsed.detail ??
            localizeHttpClientMessage(err.message, err.status),
          life: 6000,
        });
        return throwError(() => err);
      }

      if (err.status === 409 && isOrderPut(req)) {
        return throwError(() => err);
      }

      if (isProblem && err.error && typeof err.error === 'object') {
        const { summary, detail } = parseProblem(err);
        messages.add({
          severity: 'error',
          summary,
          detail,
          life: 6000,
        });
      } else if (err.status >= 400 || err.status === 0) {
        const plain = summaryAndDetailForPlainHttpError(err.status, err.message);
        messages.add({
          severity: 'error',
          summary: plain.summary,
          detail: plain.detail,
          life: 5000,
        });
      }

      return throwError(() => err);
    }),
  );
};
