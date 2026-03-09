import typescript from "@rollup/plugin-typescript";

export default {
  input: "guest-js/index.ts",
  output: {
    dir: "dist-js",
    format: "esm",
    sourcemap: true,
  },
  plugins: [
    typescript({
      tsconfig: "guest-js/tsconfig.json",
      declaration: true,
      declarationDir: "dist-js",
    }),
  ],
  external: [/^@tauri-apps\//],
};
