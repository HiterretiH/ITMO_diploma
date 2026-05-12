import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { environment } from '../../environments/environment';
import { handleMockApiRequest } from './testing/mock-api-handlers';

export const apiMockInterceptor: HttpInterceptorFn = (req, next) => {
  if (!environment.useHttpMocks) {
    return next(req);
  }
  const res = handleMockApiRequest(req);
  if (!res) {
    let path = req.url;
    try {
      path = new URL(req.url, 'http://local.invalid').pathname;
    } catch {
      /* keep path as req.url */
    }
    console.warn('[api-mock] unhandled request', req.method, path);
    return of(
      new HttpResponse({
        status: 404,
        body: { message: 'No mock handler', method: req.method, path },
      }),
    );
  }
  return of(res);
};
