import { ADMIN_ROUTES } from "constants/routeConstants";

export const ROLES = {
  ADMIN: 'ADMIN',
  MANAGER: 'MANAGER',
  SUPPLIER: 'SUPPLIER',
  STAFF: 'STAFF',
  USER: 'USER'
};

export const ROLE_ROUTES = {
  [ROLES.ADMIN]: ADMIN_ROUTES.PORTAL,
  [ROLES.MANAGER]: ADMIN_ROUTES.PORTAL,
  [ROLES.SUPPLIER]: ADMIN_ROUTES.PORTAL,
  [ROLES.STAFF]: ADMIN_ROUTES.PORTAL,
  [ROLES.USER]: '/'
};

export const ROLE_PERMISSIONS = {
  [ROLES.ADMIN]: ['all', 'access_admin'],
  [ROLES.MANAGER]: ['manage_orders', 'manage_users', 'view_reports', 'access_admin'],
  [ROLES.SUPPLIER]: ['manage_products', 'view_inventory', 'access_admin'],
  [ROLES.STAFF]: ['process_orders', 'view_products', 'access_admin'],
  [ROLES.USER]: ['view_products', 'place_orders']
};

export const ROLE_MESSAGES = {
  [ROLES.ADMIN]: 'Chào mừng Quản trị viên!',
  [ROLES.MANAGER]: 'Chào mừng Quản lý!',
  [ROLES.SUPPLIER]: 'Chào mừng Nhà cung cấp!',
  [ROLES.STAFF]: 'Chào mừng Nhân viên!',
  [ROLES.USER]: 'Đăng nhập thành công!'
};
