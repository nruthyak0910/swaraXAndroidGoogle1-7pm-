import { ScamEducationCard } from '../types';

export const SCAM_EDUCATION_DATA: ScamEducationCard[] = [
  {
    id: 'otp-scams',
    title: 'OTP & Verification Code Scams',
    tag: 'Banking & Credentials',
    summary: 'Fraudsters pose as bank officials or service providers claiming urgent account updates to steal SMS verification codes.',
    howItWorks: 'The caller initiates a transaction using your phone number or card and then urgently calls you claiming your account will be frozen or blocked unless you read out the 6-digit OTP just sent to your phone.',
    warningSigns: [
      'Caller demands that you read out a 6-digit SMS code immediately',
      'Threat of account suspension within 15 to 30 minutes',
      'Caller insists they work in "fraud prevention" or "card security"',
      'High background noise or aggressive tone creating artificial panic'
    ],
    whatToDo: [
      'Never read out or share any OTP, PIN, or CVV with any caller.',
      'Banks will never phone you asking for one-time passwords.',
      'Disconnect the call immediately and check your official mobile banking app.'
    ]
  },
  {
    id: 'upi-payment-scams',
    title: 'UPI & QR Payment Requests',
    tag: 'Digital Payments',
    summary: 'Scammers send collect requests or fake refunds on PhonePe, Google Pay, or Paytm claiming you will receive money.',
    howItWorks: 'Under the pretext of sending prize money, refunds, or paying for goods on online classifieds, the caller sends a UPI "Collect Request" and asks you to enter your UPI PIN to "receive" the money.',
    warningSigns: [
      'Caller says "Enter your UPI PIN to receive money into your bank account"',
      'Caller asks you to scan a QR code sent via WhatsApp',
      'Pressure to complete the payment authorization within seconds'
    ],
    whatToDo: [
      'Remember: You NEVER need to enter a UPI PIN to receive money.',
      'Decline all unexpected collect requests in your payment applications.',
      'Report the fraudulent VPA inside your banking/UPI application.'
    ]
  },
  {
    id: 'kyc-scams',
    title: 'KYC & SIM Deactivation Scams',
    tag: 'Telecom & Identity',
    summary: 'Callers claim your SIM card, telecom account, or banking KYC has expired and will be disconnected.',
    howItWorks: 'You receive an urgent call claiming your Aadhaar/PAN KYC is pending. The caller instructs you to install screen-sharing software (like AnyDesk or TeamViewer) or send an SMS from your device to "reactivate" service.',
    warningSigns: [
      'Threat that SIM card will be deactivated by midnight',
      'Instruction to download remote support or screen sharing apps',
      'Request to make a nominal ₹10 test transaction via an unverified link'
    ],
    whatToDo: [
      'Do not install any remote assistance apps requested over the phone.',
      'Visit your telecom provider\'s official store or official app to verify status.',
      'Do not tap external links sent via SMS claiming to update KYC.'
    ]
  },
  {
    id: 'police-govt-scams',
    title: 'Fake Police & "Digital Arrest" Scams',
    tag: 'Impersonation & Coercion',
    summary: 'Criminals impersonate police officers, CBI, or customs officials claiming you are involved in money laundering.',
    howItWorks: 'Scammers claim an illegal parcel containing contraband has been intercepted in your name, or a warrant is issued. They threaten immediate arrest and demand that you transfer funds to a "government verification account" to clear your name.',
    warningSigns: [
      'Caller claims to be from Mumbai Police, CBI, ED, or Customs',
      'Demand to stay on continuous call without notifying family or legal counsel',
      'Pressure to transfer money for "safekeeping" or "anti-money-laundering audit"'
    ],
    whatToDo: [
      'Law enforcement will never demand money transfers over a phone call.',
      'Hang up immediately and contact your local police station or dial 1930.',
      'Report incidents to the National Cyber Crime Reporting Portal (cybercrime.gov.in).'
    ]
  },
  {
    id: 'job-investment-scams',
    title: 'Work-from-Home & Investment Scams',
    tag: 'Employment & Crypto',
    summary: 'Promised high returns for simple tasks like liking videos, followed by demands to deposit funds into fake trading portals.',
    howItWorks: 'Begins with small payouts for trivial tasks on Telegram or WhatsApp. Once trust is built, the victim is prompted to invest larger sums into a fraudulent trading platform that disables withdrawals.',
    warningSigns: [
      'Unsolicited job offers offering ₹5,000–₹10,000 daily for clicking links',
      'Requirement to pay a "security deposit" or "crypto training fee"',
      'Guaranteed high daily returns with zero risk'
    ],
    whatToDo: [
      'Legitimate employers never charge candidates money for hiring or equipment.',
      'Do not transfer money to personal bank accounts for promised task returns.'
    ]
  },
  {
    id: 'delivery-courier-scams',
    title: 'Delivery & Courier Impersonation',
    tag: 'Logistics',
    summary: 'Fake courier agents claiming address issues require payment of a small fee to release your package.',
    howItWorks: 'You receive a call stating a courier package cannot be delivered due to an incomplete address. The caller sends a phishing link to "update your address" and asks for a small fee which steals card credentials.',
    warningSigns: [
      'Calls about unexpected packages you never ordered',
      'Demands for a ₹5 or ₹10 address update charge',
      'Urgent links sent over SMS with mismatched web addresses'
    ],
    whatToDo: [
      'Only track shipments on the official website or app of the merchant you purchased from.',
      'Never input debit/credit card details on third-party links received via SMS.'
    ]
  },
  {
    id: 'ai-voice-scams',
    title: 'Synthetic & AI Voice Cloning Scams',
    tag: 'Emerging AI Threats',
    summary: 'Scammers use short audio clips from social media to clone the voice of a family member in apparent distress.',
    howItWorks: 'The caller uses an AI voice clone of a relative claiming an emergency (car accident, legal trouble) and begs for immediate money transfers while urging secrecy.',
    warningSigns: [
      'Unusual emotional urgency combined with an insistence on wire or UPI transfers',
      'Caller avoids answering specific family verification questions',
      'Audio cuts out or sounds unusually muffled or repetitive'
    ],
    whatToDo: [
      'Hang up and immediately call the relative back on their known direct phone number.',
      'Establish a family "safe word" or ask a personal question only they would know.',
      'Never wire funds under pressure without verifying in person or via trusted family.'
    ]
  }
];
