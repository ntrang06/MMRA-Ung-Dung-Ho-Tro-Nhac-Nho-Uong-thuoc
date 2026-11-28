import { auth } from "../firebase/config";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "https://mmra-api.onrender.com";

export class ApiError extends Error {
  status: number;
  data?: any;

  constructor(status: number, message: string, data?: any) {
    super(message);
    this.status = status;
    this.data = data;
    this.name = "ApiError";
  }
}

export async function apiFetch<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const user = auth.currentUser;
  let token: string | undefined;

  if (user) {
    try {
      token = await user.getIdToken();
    } catch (error) {
      console.error("Không lấy được Firebase ID token", error);
    }
  }

  const headers = new Headers(options.headers || {});
  headers.set("Content-Type", "application/json");

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const url = `${API_BASE_URL}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch (error) {
    throw new ApiError(0, "Không kết nối được máy chủ. Vui lòng kiểm tra kết nối mạng.");
  }

  if (response.status === 401) {
    throw new ApiError(401, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
  }

  if (response.status === 403) {
    throw new ApiError(403, "Bạn không có quyền truy cập chức năng quản trị này.");
  }

  if (!response.ok) {
    let errorData: any;
    try {
      errorData = await response.json();
    } catch {
      errorData = null;
    }

    const message =
      errorData?.detail?.message ||
      errorData?.detail ||
      `Yêu cầu thất bại (HTTP ${response.status})`;

    throw new ApiError(response.status, typeof message === "string" ? message : JSON.stringify(message), errorData);
  }

  if (response.status === 204) {
    return {} as T;
  }

  try {
    return await response.json();
  } catch {
    return {} as T;
  }
}
