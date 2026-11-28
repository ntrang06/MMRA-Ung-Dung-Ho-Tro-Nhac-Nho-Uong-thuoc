import { useState, useEffect } from "react";
import { auth, googleProvider } from "./firebase/config";
import { signInWithPopup, signInWithEmailAndPassword, signOut, onAuthStateChanged } from "firebase/auth";
import { apiFetch } from "./api/client";
import "./App.css";

type Page = "dashboard" | "medicines" | "audit";

type MenuItem = {
  id: Page;
  label: string;
  icon: string;
};

const menuItems: MenuItem[] = [
  { id: "dashboard", label: "Tổng quan", icon: "▦" },
  { id: "medicines", label: "Danh mục thuốc", icon: "▤" },
  { id: "audit", label: "Nhật ký quản trị", icon: "◷" },
];

const pageTitles: Record<Page, string> = {
  dashboard: "Tổng quan hệ thống",
  medicines: "Quản lý danh mục thuốc",
  audit: "Nhật ký quản trị",
};

interface AdminSummary {
  totalMedicines: number;
  activeMedicines: number;
  inactiveMedicines: number;
  totalAuditLogs: number;
  apiStatus: string;
}

interface CatalogMedicine {
  catalogId: string;
  name: string;
  strength: string;
  dosageForm: string;
  source: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  updatedByUid: string;
}

interface AuditLogItem {
  logId: string;
  actorUid: string;
  action: string;
  resourceType: string;
  resourceId: string;
  createdAt: string;
}

function EmptyState({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">▤</div>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}

function DashboardPage() {
  const [summary, setSummary] = useState<AdminSummary | null>(null);
  const [loadingSummary, setLoadingSummary] = useState<boolean>(true);
  const [summaryError, setSummaryError] = useState<string | null>(null);

  const [recentLogs, setRecentLogs] = useState<AuditLogItem[]>([]);
  const [loadingLogs, setLoadingLogs] = useState<boolean>(true);

  const fetchDashboardData = () => {
    setLoadingSummary(true);
    setSummaryError(null);

    apiFetch<AdminSummary>("/api/v1/admin/summary")
      .then((res) => {
        setSummary(res);
      })
      .catch((err) => {
        setSummaryError(err.message || "Không tải được thống kê hệ thống.");
      })
      .finally(() => {
        setLoadingSummary(false);
      });

    apiFetch<{ items: AuditLogItem[] }>("/api/v1/admin/audit-logs?limit=5")
      .then((res) => {
        setRecentLogs(res.items || []);
      })
      .catch((err) => {
        console.error("Lỗi tải nhật ký gần đây", err);
      })
      .finally(() => {
        setLoadingLogs(false);
      });
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  return (
    <div className="page-content">
      <div className="page-heading">
        <div>
          <h2>Tổng quan hệ thống</h2>
          <p>
            Theo dõi danh mục thuốc dùng chung và hoạt động quản trị của MMRA.
          </p>
        </div>
        <button className="primary-button" onClick={fetchDashboardData} disabled={loadingSummary}>
          {loadingSummary ? "Đang làm mới..." : "Làm mới số liệu"}
        </button>
      </div>

      {summaryError && (
        <div style={{ background: "#FEE2E2", color: "#DC2626", padding: "12px 16px", borderRadius: "10px", marginBottom: "16px", fontSize: "14px" }}>
          {summaryError}
        </div>
      )}

      <div className="stats-grid">
        <div className="stat-card">
          <span>Tổng số thuốc trong danh mục</span>
          <strong>{loadingSummary ? "..." : summary?.totalMedicines ?? 0}</strong>
          <small>Danh mục dùng chung toàn hệ thống</small>
        </div>

        <div className="stat-card">
          <span>Thuốc đang hoạt động</span>
          <strong>{loadingSummary ? "..." : summary?.activeMedicines ?? 0}</strong>
          <small>Đang hiển thị trên hệ thống</small>
        </div>

        <div className="stat-card">
          <span>Tổng số nhật ký quản trị</span>
          <strong>{loadingSummary ? "..." : summary?.totalAuditLogs ?? 0}</strong>
          <small>Ghi nhận thao tác Admin</small>
        </div>

        <div className="stat-card">
          <span>Trạng thái API Backend</span>
          <strong>{loadingSummary ? "..." : summary?.apiStatus === "healthy" ? "Khỏe mạnh (Healthy)" : summary?.apiStatus || "Ổn định"}</strong>
          <small>FastAPI Admin API</small>
        </div>
      </div>

      <section className="panel">
        <div className="panel-header">
          <h3>Hoạt động quản trị gần đây</h3>
          <span className="muted">Admin Audit Logs</span>
        </div>

        {loadingLogs ? (
          <div style={{ padding: "24px", textAlign: "center", color: "#6B7280" }}>Đang tải nhật ký...</div>
        ) : recentLogs.length === 0 ? (
          <EmptyState
            title="Chưa có nhật ký hiển thị"
            description="Các thao tác thêm, sửa, xóa thuốc của Admin sẽ được ghi nhận tại đây."
          />
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Hành động</th>
                  <th>Loại tài nguyên</th>
                  <th>Mã tài nguyên</th>
                  <th>Quản trị viên (UID)</th>
                  <th>Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {recentLogs.map((log) => (
                  <tr key={log.logId}>
                    <td>
                      <span className="status-badge" style={{ background: "#E8F7F2", color: "#0F6053" }}>
                        {log.action}
                      </span>
                    </td>
                    <td>{log.resourceType}</td>
                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{log.resourceId}</td>
                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{log.actorUid.substring(0, 10)}...</td>
                    <td>{new Date(log.createdAt).toLocaleString("vi-VN")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function MedicinesPage() {
  const [medicines, setMedicines] = useState<CatalogMedicine[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>("");

  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [editingMedicine, setEditingMedicine] = useState<CatalogMedicine | null>(null);

  const [formName, setFormName] = useState<string>("");
  const [formStrength, setFormStrength] = useState<string>("");
  const [formDosageForm, setFormDosageForm] = useState<string>("");
  const [formSource, setFormSource] = useState<string>("admin");
  const [formActive, setFormActive] = useState<boolean>(true);
  const [formSubmitting, setFormSubmitting] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);

  const fetchMedicines = (query = "") => {
    setLoading(true);
    setError(null);
    const endpoint = query ? `/api/v1/admin/medicines?search=${encodeURIComponent(query)}` : "/api/v1/admin/medicines";

    apiFetch<{ items: CatalogMedicine[] }>(endpoint)
      .then((res) => {
        setMedicines(res.items || []);
      })
      .catch((err) => {
        setError(err.message || "Không tải được danh mục thuốc.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchMedicines();
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchMedicines(searchQuery);
  };

  const openAddModal = () => {
    setEditingMedicine(null);
    setFormName("");
    setFormStrength("");
    setFormDosageForm("");
    setFormSource("admin");
    setFormActive(true);
    setFormError(null);
    setShowAddModal(true);
  };

  const openEditModal = (med: CatalogMedicine) => {
    setEditingMedicine(med);
    setFormName(med.name);
    setFormStrength(med.strength);
    setFormDosageForm(med.dosageForm);
    setFormSource(med.source);
    setFormActive(med.active);
    setFormError(null);
    setShowAddModal(true);
  };

  const handleSaveMedicine = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormSubmitting(true);
    setFormError(null);

    try {
      if (editingMedicine) {
        await apiFetch(`/api/v1/admin/medicines/${editingMedicine.catalogId}`, {
          method: "PUT",
          body: JSON.stringify({
            name: formName,
            strength: formStrength,
            dosageForm: formDosageForm,
            source: formSource,
            active: formActive,
          }),
        });
      } else {
        await apiFetch("/api/v1/admin/medicines", {
          method: "POST",
          body: JSON.stringify({
            name: formName,
            strength: formStrength,
            dosageForm: formDosageForm,
            source: formSource,
          }),
        });
      }
      setShowAddModal(false);
      fetchMedicines(searchQuery);
    } catch (err: any) {
      setFormError(err.message || "Không lưu được thông tin thuốc.");
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleDeleteMedicine = async (catalogId: string) => {
    if (!window.confirm("Bạn có chắc chắn muốn ẩn/xóa mềm thuốc này khỏi danh mục chung?")) return;

    try {
      await apiFetch(`/api/v1/admin/medicines/${catalogId}`, {
        method: "DELETE",
      });
      fetchMedicines(searchQuery);
    } catch (err: any) {
      alert(`Lỗi khi ẩn thuốc: ${err.message}`);
    }
  };

  return (
    <div className="page-content">
      <div className="page-heading">
        <div>
          <h2>Danh mục thuốc dùng chung</h2>
          <p>
            Quản lý danh mục thuốc chuẩn hóa toàn hệ thống (Admin Catalog).
          </p>
        </div>

        <button className="primary-button" onClick={openAddModal}>
          + Thêm thuốc mới
        </button>
      </div>

      {error && (
        <div style={{ background: "#FEE2E2", color: "#DC2626", padding: "12px 16px", borderRadius: "10px", marginBottom: "16px", fontSize: "14px" }}>
          {error}
        </div>
      )}

      <section className="panel">
        <form onSubmit={handleSearchSubmit} className="table-toolbar" style={{ display: "flex", gap: "10px" }}>
          <input
            type="search"
            className="search-input"
            placeholder="Tìm kiếm tên thuốc, hàm lượng, dạng..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label="Tìm kiếm thuốc"
            style={{ flex: 1, padding: "10px 14px", borderRadius: "8px", border: "1px solid #D1D5DB" }}
          />
          <button type="submit" className="primary-button" style={{ padding: "10px 18px" }}>
            Tìm kiếm
          </button>
        </form>

        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Tên thuốc</th>
                <th>Hàm lượng</th>
                <th>Dạng bào chế</th>
                <th>Nguồn</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: "center", padding: "32px", color: "#6B7280" }}>
                    Đang tải danh mục thuốc...
                  </td>
                </tr>
              ) : medicines.length === 0 ? (
                <tr>
                  <td colSpan={6}>
                    <EmptyState
                      title="Không tìm thấy thuốc"
                      description="Danh mục thuốc dùng chung hiện chưa có dữ liệu hoặc không khớp từ khóa tìm kiếm."
                    />
                  </td>
                </tr>
              ) : (
                medicines.map((med) => (
                  <tr key={med.catalogId}>
                    <td><strong>{med.name}</strong></td>
                    <td>{med.strength || "—"}</td>
                    <td>{med.dosageForm || "—"}</td>
                    <td><span className="muted">{med.source}</span></td>
                    <td>
                      <span className="status-badge" style={{ background: med.active ? "#E8F7F2" : "#FEE2E2", color: med.active ? "#0F6053" : "#DC2626" }}>
                        {med.active ? "Hoạt động" : "Tạm ẩn"}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: "flex", gap: "8px" }}>
                        <button
                          type="button"
                          className="account-button"
                          onClick={() => openEditModal(med)}
                          style={{ padding: "6px 12px", background: "#E5E7EB", color: "#1F2937", border: "none" }}
                        >
                          Sửa
                        </button>
                        <button
                          type="button"
                          className="account-button"
                          onClick={() => handleDeleteMedicine(med.catalogId)}
                          style={{ padding: "6px 12px", background: "#FEE2E2", color: "#DC2626", border: "none" }}
                        >
                          Ẩn
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {showAddModal && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }}>
          <div style={{ background: "white", padding: "24px", borderRadius: "16px", width: "450px", maxWidth: "90%" }}>
            <h3 style={{ marginBottom: "16px", fontSize: "18px", fontWeight: "bold" }}>
              {editingMedicine ? "Cập nhật thuốc trong danh mục" : "Thêm thuốc vào danh mục chung"}
            </h3>

            {formError && (
              <div style={{ background: "#FEE2E2", color: "#DC2626", padding: "10px", borderRadius: "8px", fontSize: "13px", marginBottom: "14px" }}>
                {formError}
              </div>
            )}

            <form onSubmit={handleSaveMedicine} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: 600, marginBottom: "4px" }}>Tên thuốc *</label>
                <input
                  type="text"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  required
                  placeholder="Ví dụ: Paracetamol"
                  style={{ width: "100%", padding: "10px", borderRadius: "8px", border: "1px solid #D1D5DB" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: 600, marginBottom: "4px" }}>Hàm lượng</label>
                <input
                  type="text"
                  value={formStrength}
                  onChange={(e) => setFormStrength(e.target.value)}
                  placeholder="Ví dụ: 500 mg"
                  style={{ width: "100%", padding: "10px", borderRadius: "8px", border: "1px solid #D1D5DB" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: 600, marginBottom: "4px" }}>Dạng bào chế</label>
                <input
                  type="text"
                  value={formDosageForm}
                  onChange={(e) => setFormDosageForm(e.target.value)}
                  placeholder="Ví dụ: Viên nén"
                  style={{ width: "100%", padding: "10px", borderRadius: "8px", border: "1px solid #D1D5DB" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: 600, marginBottom: "4px" }}>Nguồn dữ liệu</label>
                <input
                  type="text"
                  value={formSource}
                  onChange={(e) => setFormSource(e.target.value)}
                  placeholder="admin"
                  style={{ width: "100%", padding: "10px", borderRadius: "8px", border: "1px solid #D1D5DB" }}
                />
              </div>

              {editingMedicine && (
                <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "4px" }}>
                  <input
                    type="checkbox"
                    id="activeCheck"
                    checked={formActive}
                    onChange={(e) => setFormActive(e.target.checked)}
                  />
                  <label htmlFor="activeCheck" style={{ fontSize: "14px", fontWeight: 500 }}>Đang hoạt động</label>
                </div>
              )}

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "16px" }}>
                <button
                  type="button"
                  className="account-button"
                  onClick={() => setShowAddModal(false)}
                  style={{ padding: "10px 16px", background: "#E5E7EB", border: "none" }}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="primary-button"
                  disabled={formSubmitting}
                  style={{ padding: "10px 20px" }}
                >
                  {formSubmitting ? "Đang lưu..." : "Lưu thuốc"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<{ items: AuditLogItem[] }>("/api/v1/admin/audit-logs?limit=50")
      .then((res) => {
        setLogs(res.items || []);
      })
      .catch((err) => {
        setError(err.message || "Không tải được nhật ký quản trị.");
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <div className="page-content">
      <div className="page-heading">
        <div>
          <h2>Nhật ký quản trị</h2>
          <p>
            Theo dõi lịch sử thao tác của các quản trị viên trên hệ thống.
          </p>
        </div>
      </div>

      {error && (
        <div style={{ background: "#FEE2E2", color: "#DC2626", padding: "12px 16px", borderRadius: "10px", marginBottom: "16px", fontSize: "14px" }}>
          {error}
        </div>
      )}

      <section className="panel">
        {loading ? (
          <div style={{ padding: "32px", textAlign: "center", color: "#6B7280" }}>Đang tải nhật ký quản trị...</div>
        ) : logs.length === 0 ? (
          <EmptyState
            title="Chưa có nhật ký ghi nhận"
            description="Mọi thao tác thay đổi danh mục thuốc sẽ được ghi lại tại đây."
          />
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Hành động</th>
                  <th>Loại tài nguyên</th>
                  <th>ID tài nguyên</th>
                  <th>Admin UID</th>
                  <th>Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.logId}>
                    <td>
                      <span className="status-badge" style={{ background: "#E8F7F2", color: "#0F6053" }}>
                        {log.action}
                      </span>
                    </td>
                    <td>{log.resourceType}</td>
                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{log.resourceId}</td>
                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{log.actorUid.substring(0, 10)}...</td>
                    <td>{new Date(log.createdAt).toLocaleString("vi-VN")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function LoginPage({
  onGoogleLogin,
  onEmailLogin,
  loading,
  error,
}: {
  onGoogleLogin: () => void;
  onEmailLogin: (e: React.FormEvent<HTMLFormElement>) => void;
  loading: boolean;
  error: string | null;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-logo">M</div>

        <h1>MMRA Admin</h1>

        <p>
          Hệ thống quản trị ứng dụng hỗ trợ
          quản lý và nhắc nhở uống thuốc.
        </p>

        {error && (
          <div style={{ background: "#FEE2E2", color: "#DC2626", padding: "10px", borderRadius: "8px", fontSize: "13px", marginBottom: "16px" }}>
            {error}
          </div>
        )}

        <button
          type="button"
          className="primary-button login-button"
          onClick={onGoogleLogin}
          disabled={loading}
          style={{ marginBottom: "16px" }}
        >
          {loading ? "Đang xử lý..." : "Đăng nhập với Google"}
        </button>

        <div style={{ textAlign: "center", margin: "12px 0", color: "#6B7280", fontSize: "13px" }}>
          hoặc đăng nhập bằng email
        </div>

        <form onSubmit={onEmailLogin} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          <input
            type="email"
            placeholder="Email quản trị"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            style={{ padding: "10px 14px", borderRadius: "8px", border: "1px solid #D1D5DB", fontSize: "14px" }}
          />
          <input
            type="password"
            placeholder="Mật khẩu"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={{ padding: "10px 14px", borderRadius: "8px", border: "1px solid #D1D5DB", fontSize: "14px" }}
          />
          <button
            type="submit"
            className="primary-button"
            disabled={loading}
            style={{ marginTop: "4px" }}
          >
            {loading ? "Đang đăng nhập..." : "Đăng nhập"}
          </button>
        </form>

        <p className="login-hint" style={{ marginTop: "16px" }}>
          Yêu cầu tài khoản được cấp quyền Admin trên Firebase & FastAPI Backend.
        </p>
      </div>
    </div>
  );
}

function AccessDeniedPage({
  user,
  onSignOut,
}: {
  user: any;
  onSignOut: () => void;
}) {
  return (
    <div className="login-screen">
      <div className="login-card" style={{ textAlign: "center" }}>
        <div className="login-logo" style={{ background: "#FEE2E2", color: "#DC2626" }}>!</div>

        <h1 style={{ color: "#DC2626" }}>Không có quyền truy cập</h1>

        <p>
          Tài khoản <strong>{user?.email}</strong> đã đăng nhập thành công nhưng chưa được cấp quyền Quản trị viên (Admin).
        </p>

        <div className="login-note" style={{ background: "#FEF3C7", color: "#92400E" }}>
          Hệ thống yêu cầu Custom Claim <code>admin: true</code> được cấu hình trên Firebase Auth.
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={onSignOut}
          style={{ background: "#DC2626", marginTop: "20px" }}
        >
          Đăng xuất tài khoản khác
        </button>
      </div>
    </div>
  );
}

function App() {
  const [page, setPage] = useState<Page>("dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const [user, setUser] = useState<any>(null);
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const [checkingAuth, setCheckingAuth] = useState<boolean>(true);
  const [authError, setAuthError] = useState<string | null>(null);
  const [authLoading, setAuthLoading] = useState<boolean>(false);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser: any) => {
      setUser(firebaseUser);
      if (firebaseUser) {
        try {
          const tokenResult = await firebaseUser.getIdTokenResult(true);
          const adminClaim = tokenResult.claims.admin === true;
          setIsAdmin(adminClaim);
        } catch (err: any) {
          console.error("Lỗi kiểm tra admin claim", err);
          setIsAdmin(false);
          setAuthError("Không thể xác minh quyền quản trị từ máy chủ.");
        }
      } else {
        setIsAdmin(false);
      }
      setCheckingAuth(false);
    });

    return () => unsubscribe();
  }, []);

  const handleGoogleLogin = async () => {
    setAuthLoading(true);
    setAuthError(null);
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (err: any) {
      setAuthError(err.message || "Đăng nhập Google thất bại.");
    } finally {
      setAuthLoading(false);
    }
  };

  const handleEmailLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const emailInput = (form.elements[0] as HTMLInputElement).value;
    const passInput = (form.elements[1] as HTMLInputElement).value;

    setAuthLoading(true);
    setAuthError(null);
    try {
      await signInWithEmailAndPassword(auth, emailInput, passInput);
    } catch (err: any) {
      setAuthError(err.message || "Đăng nhập email thất bại.");
    } finally {
      setAuthLoading(false);
    }
  };

  const handleSignOut = async () => {
    try {
      await signOut(auth);
    } catch (err: any) {
      console.error("Lỗi đăng xuất", err);
    }
  };

  const changePage = (nextPage: Page) => {
    setPage(nextPage);
    setSidebarOpen(false);
  };

  if (checkingAuth) {
    return (
      <div className="app-root" style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh", background: "#F6FAF9" }}>
        <div style={{ textAlign: "center" }}>
          <div style={{ fontSize: "24px", fontWeight: "bold", color: "#0F6053", marginBottom: "12px" }}>MMRA Admin</div>
          <div style={{ color: "#6B7280" }}>Đang xác thực phiên đăng nhập...</div>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="app-root">
        <LoginPage
          onGoogleLogin={handleGoogleLogin}
          onEmailLogin={handleEmailLogin}
          loading={authLoading}
          error={authError}
        />
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="app-root">
        <AccessDeniedPage
          user={user}
          onSignOut={handleSignOut}
        />
      </div>
    );
  }

  return (
    <div className="app-root">
      <div className="admin-layout">
        <aside
          className={`sidebar ${
            sidebarOpen ? "sidebar-open" : ""
          }`}
        >
          <div className="brand">
            <div className="brand-logo">M</div>

            <div className="brand-text">
              <strong>MMRA</strong>
              <span>Admin Dashboard</span>
            </div>
          </div>

          <div className="sidebar-label">
            QUẢN TRỊ HỆ THỐNG
          </div>

          <nav
            className="sidebar-nav"
            aria-label="Điều hướng quản trị"
          >
            {menuItems.map((item) => (
              <button
                key={item.id}
                type="button"
                className={`nav-item ${
                  page === item.id ? "nav-active" : ""
                }`}
                onClick={() => changePage(item.id)}
              >
                <span className="nav-icon">
                  {item.icon}
                </span>

                <span>{item.label}</span>
              </button>
            ))}
          </nav>

          <div className="sidebar-footer">
            <span>Phiên quản trị viên</span>
            <small>{user.email}</small>
          </div>
        </aside>

        {sidebarOpen && (
          <button
            type="button"
            className="sidebar-overlay"
            aria-label="Đóng menu"
            onClick={() => setSidebarOpen(false)}
          />
        )}

        <div className="main-area">
          <header className="topbar">
            <div className="topbar-left">
              <button
                type="button"
                className="menu-toggle"
                aria-label="Mở menu"
                onClick={() => setSidebarOpen(true)}
              >
                ☰
              </button>

              <div>
                <span className="breadcrumb">
                  MMRA Admin
                </span>
                <h1>{pageTitles[page]}</h1>
              </div>
            </div>

            <div className="topbar-actions">
              <span className="status-badge" style={{ background: "#E8F7F2", color: "#0F6053" }}>
                Admin Đã Xác Thực
              </span>

              <button
                type="button"
                className="account-button"
                onClick={handleSignOut}
                style={{ background: "#FEE2E2", color: "#DC2626", border: "1px solid #FECDD3" }}
              >
                Đăng xuất
              </button>
            </div>
          </header>

          <main className="main-content">
            {page === "dashboard" && (
              <DashboardPage />
            )}

            {page === "medicines" && (
              <MedicinesPage />
            )}

            {page === "audit" && (
              <AuditLogsPage />
            )}
          </main>

          <footer className="app-footer">
            MMRA – Medication Management and
            Reminder Assistant
          </footer>
        </div>
      </div>
    </div>
  );
}

export default App;
