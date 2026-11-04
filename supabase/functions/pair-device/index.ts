import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

async function sha256(value: string): Promise<string> {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

function randomToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "POST required" }), { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } });
  }

  try {
    const body = await req.json();
    const code = String(body.code ?? "").trim();
    const displayName = String(body.displayName ?? "").trim();
    const platform = String(body.platform ?? "");
    const osVersion = String(body.osVersion ?? "");

    if (!/^\d{8}$/.test(code)) throw new Error("Enter the 8-digit pairing code.");
    if (!displayName || displayName.length > 100) throw new Error("A valid device name is required.");
    if (platform !== "android" && platform !== "ios") throw new Error("Unsupported platform.");

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!supabaseUrl || !serviceKey) throw new Error("Pairing service is not configured.");

    const admin = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
    const deviceToken = randomToken();
    const { data, error } = await admin.rpc("claim_device_pairing", {
      p_code_hash: await sha256(code),
      p_device_token_hash: await sha256(deviceToken),
      p_display_name: displayName,
      p_platform: platform,
      p_os_version: osVersion,
    });
    if (error) throw new Error(error.message);
    const result = Array.isArray(data) ? data[0] : data;
    if (!result?.device_id) throw new Error("Pairing could not be completed.");

    return new Response(JSON.stringify({
      deviceId: result.device_id,
      deviceToken,
      message: "Device paired. Keep the device token private; it is shown only once."
    }), { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json", "Cache-Control": "no-store" } });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Pairing failed.";
    return new Response(JSON.stringify({ error: message }), { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json", "Cache-Control": "no-store" } });
  }
});
