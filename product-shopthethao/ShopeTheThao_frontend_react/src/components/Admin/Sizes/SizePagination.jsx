import React from "react";
import { Select } from "antd";
import PaginationComponent from "components/User/PaginationComponent";

const SizePagination = ({ totalPages, currentPage, setCurrentPage, pageSize, handlePageSizeChange }) => {
  return (
    <div style={{ display: "flex", justifyContent: "center", marginTop: 10, gap: 10 }}>
      <PaginationComponent
        totalPages={totalPages}
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
      />
      <Select
        value={pageSize}
        style={{ width: 120, marginTop: 20 }}
        onChange={handlePageSizeChange}
      >
        <Select.Option value={5}>5 hàng</Select.Option>
        <Select.Option value={10}>10 hàng</Select.Option>
        <Select.Option value={20}>20 hàng</Select.Option>
        <Select.Option value={50}>50 hàng</Select.Option>
      </Select>
    </div>
  );
};

export default SizePagination;
