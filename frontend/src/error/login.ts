export class LoginServiceError extends Error {
  code?: number;

  constructor(message: string, code?: number) {
    super(message);
    this.name = 'LoginServiceError';
    this.code = code;
  }
}
