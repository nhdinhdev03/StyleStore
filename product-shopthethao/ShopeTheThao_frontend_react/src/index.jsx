import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App"; // Không cần '.jsx'
import reportWebVitals from "./reportWebVitals";
import "./scss/index.scss";

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(

    <App />
);

reportWebVitals();
