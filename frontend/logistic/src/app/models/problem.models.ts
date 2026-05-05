/** RFC 7807 Problem Details (application/problem+json) */
export interface ProblemDetail {
  type?: string | null;
  title?: string;
  status?: number;
  detail?: string | null;
  instance?: string | null;
  errors?: Array<{ field?: string; message?: string }> | null;
}
