import React from 'react';
import { Button, Result } from 'antd';
import { Link } from 'react-router-dom';
import { ROUTES } from 'constants/routeConstants';
const NotFound = () => (
    <Result
        status="404"
        title={<h2>Rất tiếc...</h2>}
        subTitle={
            <>
            <h3>Xin lỗi trang bạn tìm không tồn tại hoặc đang bảo trì.</h3>
            <h3>Vui lòng quay lại sau!.</h3>
          </>
        }
        extra={
            <Link to={ROUTES.HOME} type="">
                <Button>Về trang chủ</Button>
            </Link>
        }
    />
);
export default NotFound;
