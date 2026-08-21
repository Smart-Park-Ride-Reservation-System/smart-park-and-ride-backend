const fs = require('fs');
const path = 'C:/Users/Ak/Desktop/smart-park-reservation/app/slots/details/page.tsx';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(
  /setTimeLeft\(\s*HOLD_TIME_SECONDS\s*\);\s*\/\/[^\n]*\n\s*setStep\(\s*"payment"\s*\);/,
  'router.push(`/slots/gateway?reservationId=${data.reservationId}&amount=200.00`);'
);

fs.writeFileSync(path, content);
console.log("Patched successfully");
