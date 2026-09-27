#!/usr/bin/env python3
"""A stand-in for the Gemini generateContent endpoint.

Why this exists
---------------
The interesting property of the advisory path is not that it calls a model — it is what happens at
the seams: that the key is sent, that the prompt contains only assessment facts, that a rejected or
truncated response degrades to the deterministic advisory instead of publishing nothing, and that the
console shows which of the two wrote the text. Every one of those can be exercised without spending
quota or reaching the public internet, which matters on a conference network.

It is also how the integration was verified end to end: the real adapter, the real HTTP stack, the
real serialisation — only the model is fake.

Run it
------
    python3 deploy/mock-gemini/mock_gemini.py            # listens on 127.0.0.1:9099

Then point the service at it and restart:

    APP_ADVISORY_MODE=gemini \\
    APP_GEMINI_API_KEY=mock-key \\
    APP_GEMINI_ENDPOINT=http://127.0.0.1:9099 \\
    mvn -f backend/pom.xml spring-boot:run

Every assessment then reports ``"generator": "gemini"`` in ``advisoryProvenance``, with the model name
and the time the call took — the same fields a real deployment reports.

Fault injection, for demonstrating the fallback:

    MOCK_GEMINI_FAULT=500          # the model is unavailable
    MOCK_GEMINI_FAULT=429          # rate limited
    MOCK_GEMINI_FAULT=truncated    # a completion cut off before it says anything useful
    MOCK_GEMINI_FAULT=blocked      # a safety filter refuses the prompt

With any of those set, the API still answers 200 with a complete advisory, and the provenance says
the deterministic generator wrote it and why. That is the guarantee worth showing a reviewer.
"""

from __future__ import annotations

import json
import os
import re
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = os.environ.get("MOCK_GEMINI_HOST", "127.0.0.1")
PORT = int(os.environ.get("MOCK_GEMINI_PORT", "9099"))
FAULT = os.environ.get("MOCK_GEMINI_FAULT", "").strip().lower()

# The mock reads the storm identifier out of the prompt so the advisory it returns is about the storm
# it was actually asked about: the server validates that the model names the storm before publishing,
# so a canned answer would fail that check and fall back, proving nothing.
STORM_ID = re.compile(r"Storm identifier:\s*([A-Za-z0-9._-]+)")

# The prompt names the language in the language itself — "in తెలుగు (te)" — so the code, not the
# display name, is what identifies it.
LANGUAGE = re.compile(r"\bin\s+.*?\(([a-z]{2})\)\.")
ASSETS = re.compile(r"^(\d+)\. (CRITICAL|HIGH|MEDIUM|LOW) — (.+?) \[(.+?)\], (\d+) nm, (\d+) kt", re.MULTILINE)
ACTIONABLE = re.compile(r"Assets requiring pre-landfall action:\s*(\d+)")
CENTRE = re.compile(r"Storm centre:\s*([-\d.]+), ([-\d.]+)")
INTENSITY = re.compile(r"Intensity:\s*(\d+) kt, (.+?), central pressure (\d+) mb")

# Per-language sentence templates.
#
# The real prompt asks the model to write in the requested language, so the mock has to as well:
# answering a Telugu request in English would make the console look as though it ignores the
# language, and would hide the one thing worth checking — that ``/language`` on the request changes
# what comes back. Storm identifiers, units and levels stay as given in every language, exactly as
# the prompt instructs.
TEMPLATES = {
    "en": {
        "headline": "CYCLONE IMPACT ADVISORY - {storm}",
        "state": (
            "At the validity time the centre is interpolated at {lat}, {lon} with {wind} kt, "
            "{category}, central pressure {mb} mb."
        ),
        "action": "{count} asset(s) require pre-landfall action.",
        "asset": "{level}: {name} [{asset_type}] at {nm} nm, an estimated {wind} kt.",
        "instruction": "Advise all field teams to act on the listed assets and confirm readiness.",
        "closing": "Basis: the modelled wind field, interpolated between the published fixes.",
    },
    "hi": {
        "headline": "चक्रवात प्रभाव परामर्श - {storm}",
        "state": (
            "वैधता समय पर केंद्र {lat}, {lon} पर प्रक्षेपित है, जहाँ हवा {wind} kt, {category} और "
            "केंद्रीय दाब {mb} mb है।"
        ),
        "action": "{count} परिसंपत्ति(यों) के लिए भूस्पर्शन से पहले कार्रवाई आवश्यक है।",
        "asset": "{level}: {name} [{asset_type}] {nm} nm पर, अनुमानित {wind} kt।",
        "instruction": "सभी क्षेत्रीय दलों को सूचीबद्ध परिसंपत्तियों पर कार्रवाई करने और तैयारी की पुष्टि करने की सलाह दी जाती है।",
        "closing": "यह सलाह मॉडल किए गए हवा के क्षेत्र पर आधारित है, जो प्रकाशित बिंदुओं के बीच प्रक्षेपित है।",
    },
    "te": {
        "headline": "తుఫాను ప్రభావ హెచ్చరిక - {storm}",
        "state": (
            "వల్ల కాలంలో కేంద్రం {lat}, {lon} వద్ద ఇంటర్‌పోలేట్ చేయబడింది; గాలి వేగం {wind} kt, "
            "{category}, కేంద్ర పీడనం {mb} mb."
        ),
        "action": "{count} ఆస్తులకు తీరం తాకే ముందు చర్య అవసరం.",
        "asset": "{level}: {name} [{asset_type}] {nm} nm వద్ద, అంచనా {wind} kt.",
        "instruction": "అన్ని క్షేత్ర బృందాలు జాబితా చేసిన ఆస్తులపై చర్య తీసుకోవాలని, సంసిద్ధతను నిర్ధారించాలని సూచించబడింది.",
        "closing": "ఈ హెచ్చరిక అంచనా వేసిన గాలి క్షేత్రంపై ఆధారపడింది, ఇది ప్రచురిత స్థానాల మధ్య ఇంటర్‌పోలేట్ చేయబడింది.",
    },
}


def advisory_from_prompt(prompt: str) -> str:
    """Composes an advisory that only states facts present in the prompt."""
    storm = (STORM_ID.search(prompt) or [None, "UNKNOWN"])[1]
    language_match = LANGUAGE.search(prompt)
    language = language_match.group(1) if language_match else "en"
    template = TEMPLATES.get(language, TEMPLATES["en"])

    centre = CENTRE.search(prompt)
    intensity = INTENSITY.search(prompt)
    actionable = ACTIONABLE.search(prompt)
    assets = ASSETS.findall(prompt)

    lines = [template["headline"].format(storm=storm)]
    if centre and intensity:
        lines.append(
            template["state"].format(
                lat=centre[1], lon=centre[2], wind=intensity[1], category=intensity[2], mb=intensity[3]
            )
        )
    count = actionable[1] if actionable else str(len(assets))
    lines.append(template["action"].format(count=count))
    for _, level, name, asset_type, distance, wind in assets:
        lines.append(
            template["asset"].format(
                level=level, name=name, asset_type=asset_type, nm=distance, wind=wind
            )
        )
    lines.append(template["instruction"])
    lines.append(template["closing"])
    return "\n".join(lines)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_POST(self) -> None:  # noqa: N802 - the BaseHTTPRequestHandler API
        try:
            raw = self._read_body()
        except ValueError as malformed:
            self._json(400, {"error": {"message": f"malformed body: {malformed}"}})
            return

        key = self.headers.get("x-goog-api-key", "")
        # Whether the key was sent is part of what is being verified, so it is logged rather than
        # assumed — and only its shape is printed, never the value.
        print(
            f"POST {self.path} | key={'present(' + str(len(key)) + ' chars)' if key else 'MISSING'} "
            f"| {len(raw)} bytes | {self.headers.get('Transfer-Encoding') or 'content-length'}",
            flush=True,
        )

        if FAULT in {"500", "429"}:
            self._json(
                int(FAULT),
                {"error": {"code": int(FAULT), "message": "injected fault", "status": "INJECTED"}},
            )
            return

        if FAULT == "blocked":
            self._json(200, {"promptFeedback": {"blockReason": "SAFETY"}})
            return

        try:
            body = json.loads(raw)
            parts = body["contents"][0]["parts"]
            prompt = "\n".join(part.get("text", "") for part in parts)
        except (KeyError, IndexError, ValueError) as failure:
            self._json(400, {"error": {"message": f"unparseable request: {failure}"}})
            return

        if not key:
            self._json(401, {"error": {"message": "no API key header was sent"}})
            return

        text = advisory_from_prompt(prompt)
        if FAULT == "truncated":
            text = text[:20]

        self._json(
            200,
            {
                "candidates": [
                    {
                        "content": {"role": "model", "parts": [{"text": text}]},
                        "finishReason": "STOP",
                        "index": 0,
                    }
                ],
                "usageMetadata": {
                    "promptTokenCount": len(prompt) // 4,
                    "candidatesTokenCount": len(text) // 4,
                    "totalTokenCount": (len(prompt) + len(text)) // 4,
                },
                "modelVersion": "mock-gemini-1",
            },
        )

    def do_GET(self) -> None:  # noqa: N802
        self._json(200, {"status": "mock gemini is running", "fault": FAULT or "none"})

    def _read_body(self) -> bytes:
        """Reads the request body, chunked or not.

        The Java HTTP client streams a JSON body with ``Transfer-Encoding: chunked`` rather than
        setting a length, so a handler that only honours Content-Length reads zero bytes and then
        mistakes the first chunk header for the next request line. Both framings are handled here.
        """
        if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
            body = bytearray()
            while True:
                size_line = self.rfile.readline().strip()
                if not size_line:
                    break
                size = int(size_line.split(b";", 1)[0], 16)
                if size == 0:
                    self.rfile.readline()  # the CRLF that ends the last chunk
                    break
                body += self.rfile.read(size)
                self.rfile.read(2)  # CRLF after each chunk
            return bytes(body)

        length = int(self.headers.get("Content-Length", "0"))
        return self.rfile.read(length)

    def _json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt: str, *args: object) -> None:
        # Replaces the default stderr line with something that reads as a request log.
        print(f"  {fmt % args}", flush=True)


def main() -> int:
    if FAULT:
        print(f"Fault injection active: MOCK_GEMINI_FAULT={FAULT}")
    print(f"Mock Gemini listening on http://{HOST}:{PORT} (POST /models/<model>:generateContent)")
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
