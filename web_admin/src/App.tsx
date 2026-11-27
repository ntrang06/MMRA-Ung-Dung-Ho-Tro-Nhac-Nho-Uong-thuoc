
import { useState } from "react";
import "./App.css";

type Page = "login" | "dashboard" | "medicines" | "audit";

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
  login: "Đăng nhập quản trị",
  dashboard: "Tổng quan hệ thống",
  medicines: "Quản lý danh mục thuốc",
  audit: "Nhật ký quản trị",
};

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
  return (
    <div className="page-content">
      <div className="page-heading">
        <div>
          <h2>Tổng quan hệ thống</h2>
          <p>
            Theo dõi danh mục thuốc và hoạt động của MMRA.
          </p>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <span>Tổng số thuốc trong danh mục</span>
          <strong>—</strong>
          <small>Chưa kết nối Backend</small>
        </div>

        <div className="stat-card">
          <span>Thuốc đang hoạt động</span>
          <strong>—</strong>
          <small>Chưa kết nối Backend</small>
        </div>

        <div className="stat-card">
          <span>Trạng thái API</span>
          <strong>—</strong>
          <small>Chưa kiểm tra kết nối</small>
        </div>
      </div>

      <section className="panel">
        <div className="panel-header">
          <h3>Hoạt động gần đây</h3>
          <span className="muted">Dữ liệu hệ thống</span>
        </div>

        <EmptyState
          title="Chưa có dữ liệu hiển thị"
          description="Thông tin sẽ được tải từ Admin API sau khi tích hợp."
        />
      </section>
    </div>
  );
}

function MedicinesPage() {
  return (
    <div className="page-content">
      <div className="page-heading">
        <div>
          <h2>Danh mục thuốc</h2>
          <p>
            Quản lý danh mục thuốc dùng chung của hệ thống.
          </p>
        </div>

        <button className="primary-button" disabled>
          + Thêm thuốc
        </button>
      </div>

      <section className="panel">
        <div className="table-toolbar">
          <input
            type="search"
            className="search-input"
            placeholder="Tìm kiếm tên thuốc..."
            aria-label="Tìm kiếm thuốc"
            disabled
          />

          <span className="muted">
            Chưa kết nối Admin API
          </span>
        </div>

        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Tên thuốc</th>
                <th>Hàm lượng</th>
                <th>Dạng thuốc</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td colSpan={5}>
                  <EmptyState
                    title="Chưa tải danh mục thuốc"
                    description="Danh sách sẽ hiển thị dữ liệu thực tế từ Backend."
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function AuditLogsPage() {
  return (
    <div className="page-content">
      <div className="page-heading">
        <div>
          <h2>Nhật ký quản trị</h2>
          <p>
            Theo dõi các thao tác quản lý hệ thống.
          </p>
        </div>
      </div>

      <section className="panel">
        <EmptyState
          title="Chưa có nhật ký hiển thị"
          description="Nhật ký sẽ được lấy từ API quản trị sau khi kết nối."
        />
      </section>
    </div>
  );
}

function LoginPage({
  onBack,
}: {
  onBack: () => void;
}) {
  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-logo">M</div>

        <h1>MMRA Admin</h1>

        <p>
          Hệ thống quản trị ứng dụng hỗ trợ
          quản lý và nhắc nhở uống thuốc.
        </p>

        <div className="login-note">
          Đăng nhập quản trị sẽ sử dụng Firebase
          Authentication và xác minh quyền Admin
          thông qua FastAPI Backend.
        </div>

        <button
          className="primary-button login-button"
          disabled
        >
          Đăng nhập với Firebase
        </button>

        <p className="login-hint">
          Chưa tích hợp đăng nhập thật.
          Đây chỉ là màn hình mẫu.
        </p>

        <button
          className="back-button"
          onClick={onBack}
        >
          ← Quay lại Dashboard mẫu
        </button>
      </div>
    </div>
  );
}

function App() {
  const [page, setPage] = useState<Page>("dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const changePage = (nextPage: Page) => {
    setPage(nextPage);
    setSidebarOpen(false);
  };

  if (page === "login") {
    return (
      <div className="app-root">
        <div className="preview-banner">
          BẢN XEM TRƯỚC – Chưa tích hợp xác thực
        </div>

        <LoginPage
          onBack={() => changePage("dashboard")}
        />
      </div>
    );
  }

  return (
    <div className="app-root">
      <div className="preview-banner">
        BẢN XEM TRƯỚC – Chưa đăng nhập và chưa kết nối
        dữ liệu thật. Không sử dụng cho Production.
      </div>

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
            <span>Môi trường phát triển</span>
            <small>MMRA Admin – Scaffold 1.0</small>
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
              <span className="status-badge">
                Chưa kết nối API
              </span>

              <button
                type="button"
                className="account-button"
                onClick={() => changePage("login")}
              >
                Xem mẫu đăng nhập
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
