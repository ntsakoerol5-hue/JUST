import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "Private Phone Management", description: "Consent-based device management dashboard" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="en"><body>{children}</body></html>; }
