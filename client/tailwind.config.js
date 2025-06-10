/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        success: {
          400: "#10B981",
          500: "#25D366",
        },
        ruby: {
          50: "#FCEAEA",
          100: "#F8D3D3",
          200: "#F2A6A6",
          300: "#EC7A7A",
          400: "#E64D4D",
          500: "#9B111E",
          600: "#7A0E18",
          700: "#5A0A12",
          800: "#39070C",
          900: "#190306",
        },
        neutral: {
          50: "#FAFAFA",
          100: "#F5F5F5",
          200: "#EEEEEE",
          300: "#E0E0E0",
          400: "#BDBDBD",
          500: "#9E9E9E",
          600: "#757575",
          700: "#616161",
          800: "#424242",
          900: "#212121",
        },
      },
      fontFamily: {
        sans: ["Inter", "Helvetica Neue", "Arial", "sans-serif"],
      },
      fontSize: {
        xs: ["10px", { lineHeight: "1.2" }],
        sm: ["12px", { lineHeight: "1.4" }],
        base: ["14px", { lineHeight: "1.4" }],
        lg: ["16px", { lineHeight: "1.4" }],
        xl: ["18px", { lineHeight: "1.2" }],
      },
      borderRadius: {
        lg: "8px",
        xl: "12px",
        "2xl": "16px",
        full: "50%",
      },
      spacing: {
        18: "4.5rem",
      },
    },
  },
  plugins: [],
};
