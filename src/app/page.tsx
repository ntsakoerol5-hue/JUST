"use client";

import { useEffect, useState } from "react";
import { createClient, SupabaseClient, User } from "@supabase/supabase-js";

export default function Home() {
  const [client, setClient] = useState<SupabaseClient | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [pairingCode, setPairingCode] = useState("");
  const [expiresAt, setExpiresAt] = useState("");

  useEffect(() => {
    const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
    const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
    if (!url || !key) return;
    const supabase = createClient(url, key);
    setClient(supabase);
    supabase.auth.getSession().then(({ data }) => setUser(data.session?.user ?? null));
    const { data: listener } = supabase.auth.onAuthStateChange((_event, session) => setUser(session?.user ?? null));
    return () => listener.subscription.unsubscribe();
  }, []);

  async function authenticate(mode: "sign-in" | "sign-up") {
    if (!client) return;
    setBusy(true); setMessage("");
    const result = mode === "sign-in"
      ? await client.auth.signInWithPassword({ email, password })
      : await client.auth.signUp({ email, password });
    setBusy(false);
    if (result.error) setMessage(result.error.message);
    else if (mode === "sign-up" && !result.data.session) setMessage("Check your email to confirm your account, then sign in.");
    else setMessage(mode === "sign-up" ? "Account created." : "Signed in.");
  }

  async function createPairingCode() {
    if (!client) return;
    setBusy(true); setMessage(""); setPairingCode("");
    const { data, error } = await client.rpc("create_device_pairing_code");
    setBusy(false);
    if (error) { setMessage(error.message); return; }
    const row = Array.isArray(data) ? data[0] : data;
    if (!row?.pairing_code) { setMessage("No pairing code was returned."); return; }
    setPairingCode(String(row.pairing_code));
    setExpiresAt(String(row.expires_at ?? ""));
    setMessage("Enter this code in JUST Companion on the phone you are enrolling.");
  }

  async function signOut() {
    if (!client) return;
    await client.auth.signOut();
    setPairingCode(""); setExpiresAt(""); setMessage("Signed out.");
  }

  if (!client) return <main className="shell"><section className="content"><h1>JUST</h1><div className="notice"><b>Supabase setup required</b><span>Add NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_ANON_KEY to your environment, then restart the app.</span></div></section></main>;

  return <main className="shell">
    <aside className="sidebar">
      <div className="brand"><div className="brandIcon">J</div><div><b>JUST</b><small>PRIVATE CONSOLE</small></div></div>
      <nav><a className="active">▦　Overview</a><a>▣　Devices</a><a>⌖　Shared data</a><a>◷　Activity log</a><a>⚙　Settings</a></nav>
      <div className="sideNote"><span className="dot"/> Secure workspace<br/><small>Owner-controlled device access</small></div>
    </aside>
    <section className="content">
      <header><div><div className="eyebrow">WORKSPACE / OVERVIEW</div><h1>Device management</h1><p className="muted">Pair a phone using a short-lived code. The phone owner chooses what to share.</p></div><div className="profile"><div className="avatar">{user ? "✓" : "?"}</div><span>{user ? user.email : "Not signed in"}<br/><small>{user ? "Account connected" : "Preview mode"}</small></span></div></header>
      {!user ? <section className="getting">
        <h2>Sign in to JUST</h2><p className="muted">Create an account or sign in to generate a phone-pairing code.</p>
        <label>Email</label><input className="field" type="email" autoComplete="email" value={email} onChange={e=>setEmail(e.target.value)} placeholder="you@example.com"/>
        <label>Password</label><input className="field" type="password" autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="At least 6 characters"/>
        <div className="actions"><button disabled={busy || !email || password.length < 6} onClick={()=>authenticate("sign-in")}>Sign in</button><button className="secondary" disabled={busy || !email || password.length < 6} onClick={()=>authenticate("sign-up")}>Create account</button></div>
      </section> : <>
        <div className="notice"><b>Account connected</b><span>Only devices paired to your account will appear here. No phone is currently connected by this screen.</span></div>
        <section className="getting">
          <div className="sectionHead"><div><h2>Pair an Android phone</h2><p className="muted">Generate a one-time code, then enter it in JUST Companion.</p></div><button onClick={createPairingCode} disabled={busy}>{busy ? "Please wait…" : "＋ Generate pairing code"}</button></div>
          {pairingCode && <div className="pairCode"><span>ONE-TIME PAIRING CODE</span><strong>{pairingCode}</strong><small>{expiresAt ? "Expires: " + new Date(expiresAt).toLocaleString() : "Expires in 10 minutes"}</small></div>}
          <p className="muted">Codes expire after 10 minutes and can only be used once. Keep the code private and enter it only on the phone you intend to enrol.</p>
        </section>
        <section className="getting"><h2>Connected devices</h2><p className="muted">Your enrolled devices will be listed here after the companion pairing flow is completed.</p><div className="notice"><span>No devices loaded yet. This dashboard does not display demo device information as real data.</span></div></section>
        <button className="secondary" onClick={signOut}>Sign out</button>
      </>}
      {message && <p className="statusMessage" role="status">{message}</p>}
      <footer>JUST · Privacy by design · You control device sharing</footer>
    </section>
  </main>;
}
