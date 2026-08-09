import axios from "axios";
import { apiClient, BASE_URL } from "./client";
import type {
  ApiResponse,
  AuthTokens,
  LoginRequest,
  RegisterRequest,
  UserProfile,
} from "../types";

export const authApi = {
  login: (body: LoginRequest) =>
    apiClient
      .post<ApiResponse<AuthTokens>>("/api/v1/auth/login", body)
      .then((r) => r.data),

  // apiClientのインターセプターを経由すると401→再refresh→無限ループになりうるため、素のaxiosを使う
  refresh: (refreshToken: string) =>
    axios
      .post<ApiResponse<AuthTokens>>(`${BASE_URL}/api/v1/auth/refresh`, { refreshToken })
      .then((r) => r.data),

  register: (body: RegisterRequest) =>
    apiClient
      .post<ApiResponse<null>>("/api/v1/users", body)
      .then((r) => r.data),

  logout: (refreshToken: string) =>
    apiClient
      .post<ApiResponse<null>>("/api/v1/auth/logout", { refreshToken })
      .then((r) => r.data),

  getMe: () =>
    apiClient
      .get<ApiResponse<UserProfile>>("/api/v1/users/me")
      .then((r) => r.data),
};
