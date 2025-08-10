import React, { useState } from "react";
import {
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  Upload,
  Checkbox,
  Row,
  Col,
  Button,
  Space,
  Tooltip,
} from "antd";
import {
  UserOutlined,
  PhoneOutlined,
  MailOutlined,
  HomeOutlined,
  PlusOutlined,
  UnlockOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import { Modal as AntModal } from 'antd';

const AccountModal = ({
  open,
  editUser,
  form,
  FileList,
  statusChecked,
  isStatusEditable,
  handleCancel,
  handleChange,
  onPreview,
  handleStatus,
  handleStatusChange,
  handleResetForm,
  handleModalOk,
}) => {
  const [isFormDisabled, setIsFormDisabled] = useState(false);
  const [isUnlocked, setIsUnlocked] = useState(false);
  const isLockedAccount = editUser && editUser.status === 0;

  const handleModalCancel = () => {
    if (isUnlocked) {
      AntModal.warning({
        title: 'Không thể hủy',
        content: 'Vui lòng cập nhật thông tin sau khi mở khóa tài khoản',
        okText: 'Đã hiểu',
      });
      return;
    }
    handleCancel();
  };

  const handleUnlockAndDisable = (lockReasonId) => {
    AntModal.confirm({
      title: 'Xác nhận mở khóa',
      content: 'Bạn có chắc chắn muốn mở khóa tài khoản này?',
      okText: 'Đồng ý',
      cancelText: 'Hủy',
      onOk: () => {
        handleStatusChange(lockReasonId);
        setIsFormDisabled(false);
        setIsUnlocked(true);
        form.setFieldsValue({ status: true });
        AntModal.success({
          title: 'Mở khóa thành công',
          content: 'Vui lòng cập nhật thông tin để hoàn tất',
          okText: 'Đã hiểu',
        });
      },
    });
  };

  const onModalOk = () => {
    handleModalOk();
    setIsUnlocked(false);
  };

  return (
    <Modal
      title={editUser ? "Cập nhật tài khoản" : "Thêm tài khoản mới"}
      open={open}
      footer={null}
      onCancel={handleModalCancel}
      width={700}
    >
      <Form form={form} layout="vertical" validateTrigger="onBlur">
        <Row gutter={16}>
          {/* User Name */}
          <Col xs={24} sm={12}>
            <Form.Item
              name="id"
              label="User Name"
              rules={[{ required: true, message: "Vui lòng nhập User Name!" }]}
            >
              <Input 
                prefix={<UserOutlined />} 
                placeholder="Nhập User Name" 
                disabled={isLockedAccount || isFormDisabled}
              />
            </Form.Item>
          </Col>

          {/* Fullname */}
          <Col xs={24} sm={12}>
            <Form.Item
              name="fullname"
              label="Họ tên"
              rules={[{ required: true, message: "Vui lòng nhập họ tên!" }]}
            >
              <Input 
                prefix={<UserOutlined />} 
                placeholder="Nhập họ tên" 
                disabled={isLockedAccount || isFormDisabled}
              />
            </Form.Item>
          </Col>

          {/* Phone */}
          <Col xs={24} sm={12}>
            <Form.Item
              name="phone"
              label="Số điện thoại"
            >
              <Input
                prefix={<PhoneOutlined />}
                placeholder="Nhập số điện thoại"
                disabled={isLockedAccount || isFormDisabled}
              />
            </Form.Item>
          </Col>

          {/* Email */}
          <Col xs={24} sm={12}>
            <Form.Item
              name="email"
              label="Email"
            >
              <Input 
                prefix={<MailOutlined />} 
                placeholder="Nhập email" 
                disabled={isLockedAccount || isFormDisabled}
              />
            </Form.Item>
          </Col>

          {/* Address */}
          <Col xs={24} sm={12}>
            <Form.Item
              name="address"
              label="Địa chỉ"
            >
              <Input 
                prefix={<HomeOutlined />} 
                placeholder="Nhập địa chỉ" 
                disabled={isLockedAccount || isFormDisabled}
              />
            </Form.Item>
          </Col>

          {/* Birthday */}
          <Col xs={24} sm={12}>
            <Form.Item
              name="birthday"
              label="Ngày sinh"
            >
              <DatePicker
                format="DD/MM/YYYY"
                placeholder="Chọn ngày sinh"
                style={{ width: "100%" }}
                disabled={isLockedAccount || isFormDisabled}
              />
            </Form.Item>
          </Col>

          {/* Gender */}
          <Col xs={24} sm={12}>
            <Form.Item
              name="gender"
              label="Giới tính"
            >
              <Select 
                placeholder="Chọn giới tính"
                disabled={isLockedAccount || isFormDisabled}
              >
                <Select.Option value="M">Nam giới</Select.Option>
                <Select.Option value="F">Nữ giới</Select.Option>
                <Select.Option value="O">Khác</Select.Option>
              </Select>
            </Form.Item>
          </Col>

          {/* Image Upload */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Ảnh đại diện"
              name="image"
            >
              <Upload
                beforeUpload={() => false}
                accept=".png, .jpg"
                listType="picture-card"
                onChange={handleChange}
                onPreview={onPreview}
                fileList={FileList}
                maxCount={1}
                disabled={isLockedAccount || isFormDisabled}
              >
                {FileList.length < 1 && "+ Upload"}
              </Upload>
            </Form.Item>
          </Col>

          {/* Verified Status */}
          {editUser && (
            <Col xs={24} sm={12}>
              <Form.Item
                name="verified"
                label="Xác thực"
                valuePropName="checked"
                initialValue={true}
              >
                <Checkbox disabled={isLockedAccount || isFormDisabled}>Đã xác thực</Checkbox>
              </Form.Item>
            </Col>
          )}

          {/* Status - Làm cho checkbox không thể tương tác khi có lý do khóa */}
          {editUser && (
            <Col xs={24} sm={12}>
              <Form.Item
                name="status"
                label="Trạng thái"
                valuePropName="checked"
                initialValue={statusChecked}
              >
                <Checkbox 
                  onChange={handleStatus}
                  disabled={editUser.lockReasons?.length > 0} // Disable checkbox if there are lock reasons
                >
                  Tình Trạng Tài khoản
                </Checkbox>
              </Form.Item>
            </Col>
          )}

          {/* Lock Reason - Luôn cho phép chỉnh sửa */}
          {editUser && !statusChecked && (
            <Col span={24}>
              <Form.Item
                name="lockReasons"
                label="Lý do khóa"
                rules={[
                  {
                    required: !statusChecked,
                    message: "Vui lòng nhập lý do khóa!",
                  },
                ]}
              >
                <Input.TextArea
                  placeholder="Nhập lý do khóa"
                  rows={4}
                  defaultValue={editUser?.lockReasons?.[0]?.reason || ""}
                />
              </Form.Item>
            </Col>
          )}

          {/* Xóa lý do khóa Button - Enhanced UI */}
          {editUser && editUser.lockReasons?.length > 0 && (
            <Col span={24}>
              <div style={{ 
                padding: '16px',
                background: '#f6f6f6',
                borderRadius: '8px',
                marginBottom: '16px'
              }}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <div style={{ color: '#666', marginBottom: '8px' }}>
                    Tài khoản hiện đang bị khóa
                  </div>
                  <Tooltip title="Nhấn để mở khóa tài khoản này">
                    <Button
                      type="primary"
                      danger
                      icon={<UnlockOutlined />}
                      onClick={() => handleUnlockAndDisable(editUser.lockReasons[0].id)}
                      style={{
                        width: '100%',
                        height: '40px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '8px'
                      }}
                    >
                      Mở khóa tài khoản
                    </Button>
                  </Tooltip>
                </Space>
              </div>
            </Col>
          )}

          {/* Password */}
          {!editUser && (
            <Col xs={24}>
              <Form.Item
                name="password"
                label="Mật khẩu"
                rules={[
                  { required: true, message: "Vui lòng nhập mật khẩu!" },
                  { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự!" },
                ]}
              >
                <Input.Password 
                  placeholder="Nhập mật khẩu"
                  disabled={isLockedAccount || isFormDisabled}
                />
              </Form.Item>
            </Col>
          )}
        </Row>

        {/* Action Buttons */}
        <Space
          style={{
            display: "flex",
            justifyContent: "flex-end",
            width: "100%",
          }}
        >
          <Button 
            onClick={handleResetForm} 
            disabled={isFormDisabled || isUnlocked}
          >
            Làm mới
          </Button>
          <Button 
            type="primary" 
            onClick={onModalOk}
          >
            {editUser ? "Cập nhật" : "Thêm mới"}
          </Button>
        </Space>
      </Form>
    </Modal>
  );
};

export default AccountModal;
