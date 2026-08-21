const fs = require('fs');
const src = 'd:/Projects/smartparking/scratch_details.tsx';
const dest = 'C:/Users/Ak/Desktop/smart-park-reservation/app/slots/details/page.tsx';

let content = fs.readFileSync(src, 'utf8');

// Normalize newlines
content = content.replace(/\r\n/g, '\n');

const targetStr = `      setReservationId(data.reservationId);
      setTimeLeft(HOLD_TIME_SECONDS); // Reset hold timer to 2 minutes
      setStep("payment");`;

const replaceStr = `      // Automatically redirect to payment gateway to start the soft lock on only one page
      router.push(\`/slots/gateway?reservationId=\${data.reservationId}&amount=200.00\`);`;

if (content.includes(targetStr)) {
  content = content.replace(targetStr, replaceStr);
  fs.writeFileSync(dest, content);
  console.log("Successfully replaced and copied!");
} else {
  console.log("Could not find the target string!");
}
