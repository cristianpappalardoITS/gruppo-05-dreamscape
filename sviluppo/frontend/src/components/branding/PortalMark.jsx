/** The Dreamscape portal mark, shared by the authentication pages and home. */
export default function PortalMark({ className = '' }) {
  return (
    <svg className={className} viewBox="0 0 180 210" role="img" aria-label="Simbolo di Dreamscape">
      <g fill="none" stroke="currentColor" strokeWidth="8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M90 8 C85 31 51 60 47 92 C43 111 48 128 44 152 C42 173 32 188 7 191 C30 195 47 184 55 168 C64 148 57 120 62 95 C67 70 81 46 90 33 C99 46 113 70 118 95 C123 120 116 148 125 168 C133 184 150 195 173 191 C148 188 138 173 136 152 C132 128 137 111 133 92 C129 60 95 31 90 8 Z" />
        <path d="M58 126 C59 146 66 158 82 164 C65 168 57 183 47 188" />
        <path d="M122 126 C121 146 114 158 98 164 C115 168 123 183 133 188" />
        <path d="M64 187 C70 172 77 168 90 162 C103 168 110 172 116 187" />
      </g>
      <path d="M90 70 C94 85 99 91 108 93 C99 96 94 102 90 118 C86 102 81 96 72 93 C81 91 86 85 90 70 Z" fill="currentColor" />
    </svg>
  );
}
