"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import NavBar from "../../components/layout/NavBar";
import { getAuthHeaders, getUser } from "@/lib/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const COUNTRY_CODES: { code: string; label: string }[] = [
  { code: "+94", label: "Sri Lanka (+94)" },
  { code: "+91", label: "India (+91)" },
  { code: "+1", label: "USA (+1)" },
  { code: "+44", label: "UK (+44)" },
  { code: "+971", label: "UAE (+971)" },
];

type VehicleType = "BIKE" | "CAR" | "VAN" | "SUV" | "THREEWHEEL";

const VEHICLE_TYPES: { value: VehicleType; label: string }[] = [
  { value: "BIKE", label: "Bike" },
  { value: "CAR", label: "Car" },
  { value: "VAN", label: "Van" },
  { value: "SUV", label: "SUV" },
  { value: "THREEWHEEL", label: "Three Wheel" },
];

const DEFAULT_BOOKING_DURATION_HOURS = 2;
const HOLD_TIME_SECONDS = 120; // 2 Minutes Soft Lock

interface ReservationPayload {
  slot: { slotId: number };
  user: { userId: number };
  vehicleNo: string;
  vehicleType: VehicleType;
  scheduledIn: string;
  scheduledOut: string;
  phone?: string;
}

type Step = "details" | "payment";

export default function Page(): React.JSX.Element {
  return (
    <Suspense fallback={<DetailsFallback />}>
      <DetailsContent />
    </Suspense>
  );
}

function DetailsFallback(): React.JSX.Element {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 via-blue-50 to-slate-100">
      <div className="text-gray-400 text-sm animate-pulse">Loading booking details…</div>
    </div>
  );
}

function DetailsContent(): React.JSX.Element {
  const router = useRouter();
  const params = useSearchParams();

  const slotId = params.get("slotId");
  const slotCode = params.get("slot");
  const date = params.get("date");
  const time = params.get("time");
  const hubName = params.get("hubName");

  const [phone, setPhone] = useState<string>("");
  const [countryCode, setCountryCode] = useState<string>("+94");
  const [isEditingPhone, setIsEditingPhone] = useState<boolean>(false);

  const [savedVehicles, setSavedVehicles] = useState<string[]>([]);
  const [vehicleNo, setVehicleNo] = useState<string>("");
  const [isCustomVehicle, setIsCustomVehicle] = useState<boolean>(false);
  const [vehicleType, setVehicleType] = useState<VehicleType>("CAR");

  const [loading, setLoading] = useState<boolean>(false);
  const [payLoading, setPayLoading] = useState<boolean>(false);
  const [step, setStep] = useState<Step>("details");
  const [reservationId, setReservationId] = useState<number | null>(null);

  // Soft Lock Countdown Timer State
  const [timeLeft, setTimeLeft] = useState<number>(HOLD_TIME_SECONDS);

  // Load saved user info & vehicles on mount
  useEffect(() => {
    const user = getUser();
    if (!user) { router.push("/login"); return; }
    if (!slotId) { router.push("/"); return; }

    async function fetchUserData() {
      try {
        const res = await fetch(`${API_URL}/api/users/me`, {
          headers: getAuthHeaders(),
        });
        if (res.ok) {
          const data = await res.json();
          if (data.phone) {
            setPhone(data.phone.replace(/\D/g, "").slice(-9)); // Strip formatting to local digits
          }
          if (data.vehicles && Array.isArray(data.vehicles)) {
            setSavedVehicles(data.vehicles);
            if (data.vehicles.length > 0) {
              setVehicleNo(data.vehicles[0]);
            } else {
              setIsCustomVehicle(true);
            }
          } else {
            setIsCustomVehicle(true);
          }
        }
      } catch (err) {
        console.error("Failed to load user profile", err);
      }
    }

    fetchUserData();
  }, [router, slotId]);

  // 2-Minute Timer for Payment Step
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (step === "payment" && timeLeft > 0) {
      timer = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0 && step === "payment") {
      alert("Your 2-minute soft lock hold has expired. Please select a slot again.");
      router.push("/slots");
    }
    return () => clearInterval(timer);
  }, [step, timeLeft, router]);

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
  };

  const validatePhone = (): string | null => {
    const digits = phone.replace(/\D/g, "");
    if (countryCode === "+94") {
      if (digits.length !== 9 && digits.length !== 10) return "Invalid Sri Lankan number";
    } else if (digits.length < 7 || digits.length > 15) {
      return "Invalid phone number";
    }
    return null;
  };

  const saveUserDataToProfile = async (fullPhone: string, currentVehicleNo: string) => {
    try {
      const updatedVehicles = Array.from(
        new Set([...savedVehicles, currentVehicleNo.toUpperCase()])
      );

      await fetch(`${API_URL}/api/users/me`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          ...getAuthHeaders(),
        },
        body: JSON.stringify({
          phone: fullPhone,
          vehicles: updatedVehicles,
        }),
      });
    } catch (err) {
      console.error("Failed to update user profile data", err);
    }
  };

  const handleConfirm = async (): Promise<void> => {
    const user = getUser();
    if (!user) { router.push("/login"); return; }
    if (!slotId) { alert("Missing slot — please pick a slot again."); router.push("/"); return; }
    if (!vehicleNo.trim()) { alert("Enter vehicle number"); return; }
    const phoneError = validatePhone();
    if (phoneError) { alert(phoneError); return; }

    const formattedPhone = `${countryCode}${phone.replace(/\D/g, "")}`;
    const formattedVehicleNo = vehicleNo.toUpperCase();

    const formattedTime = time ? time.trim() : "00:00";
    const scheduledInDate = new Date(`${date}T${formattedTime}:00`);
    const scheduledOutDate = new Date(
      scheduledInDate.getTime() + DEFAULT_BOOKING_DURATION_HOURS * 60 * 60 * 1000
    );

    const payload: ReservationPayload = {
      slot: { slotId: Number(slotId) },
      user: { userId: user.userId },
      vehicleNo: formattedVehicleNo,
      vehicleType,
      scheduledIn: `${date}T${formattedTime}:00`,
      scheduledOut: scheduledOutDate.toISOString().slice(0, 19),
      phone: formattedPhone,
    };

    setLoading(true);
    try {
      // 1. Create reservation
      const res = await fetch(`${API_URL}/api/reservations`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...getAuthHeaders(),
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const errorText = await res.text();
        console.error("Server validation error:", errorText);
        throw new Error("Reservation failed");
      }

      const data = await res.json();

      // 2. Persist updated phone and vehicle list back to user profile
      await saveUserDataToProfile(formattedPhone, formattedVehicleNo);

      setReservationId(data.reservationId);
      setTimeLeft(HOLD_TIME_SECONDS); // Reset hold timer to 2 minutes
      setStep("payment");
    } catch (err) {
      console.error(err);
      alert("Booking failed. Please check the console for details.");
    } finally {
      setLoading(false);
    }
  };

  const handleProceedToPayment = (): void => {
    if (!reservationId) return;
    const user = getUser();
    if (!user) { router.push("/login"); return; }

    setPayLoading(true);
    router.push(`/slots/gateway?reservationId=${reservationId}&amount=200.00`);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-100 via-blue-50 to-slate-100">
      <NavBar />

      <div className="max-w-2xl mx-auto px-4 py-10">

        <button
          onClick={() => step === "payment" ? setStep("details") : router.back()}
          className="text-sm text-gray-500 hover:text-black mb-6 transition-colors"
        >
          ← Back
        </button>

        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900">
            {step === "details" ? "Confirm Booking" : "Complete Payment"}
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            {step === "details"
              ? "Review and confirm your reservation"
              : "Your slot is temporarily soft-locked — complete payment before timer runs out"}
          </p>
        </div>

        <div className="bg-blue-600 text-white rounded-2xl p-6 mb-6 shadow-lg">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-4xl font-black">{slotCode ?? "—"}</h2>
              <p className="text-blue-100 mt-1">{hubName ?? "—"}</p>
            </div>
            <div className="text-right text-sm text-blue-100">
              <p className="font-semibold text-white">Arrival</p>
              <p>{date ?? "—"}</p>
              <p>{time ?? "—"}</p>
            </div>
          </div>
        </div>

        {step === "details" && (
          <div className="bg-white rounded-2xl shadow-md p-6 space-y-5">
            {/* Phone Number Input with Edit Mode */}
            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="block text-xs font-medium text-gray-500">Phone *</label>
                {phone && !isEditingPhone && (
                  <button
                    type="button"
                    onClick={() => setIsEditingPhone(true)}
                    className="text-xs text-blue-600 hover:underline flex items-center gap-1"
                  >
                    ✏️ Edit
                  </button>
                )}
              </div>
              <div className="flex gap-2">
                <select
                  value={countryCode}
                  onChange={(e) => setCountryCode(e.target.value)}
                  disabled={!!phone && !isEditingPhone}
                  className="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-500"
                >
                  {COUNTRY_CODES.map((c) => (
                    <option key={c.code} value={c.code}>{c.label}</option>
                  ))}
                </select>
                <input
                  type="tel"
                  value={phone}
                  readOnly={!!phone && !isEditingPhone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="Phone number"
                  className="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 read-only:bg-gray-50 read-only:text-gray-600"
                />
              </div>
            </div>

            {/* Vehicle Selection Dropdown / Input */}
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Vehicle Number *</label>
              {savedVehicles.length > 0 && !isCustomVehicle ? (
                <div className="space-y-2">
                  <select
                    value={vehicleNo}
                    onChange={(e) => {
                      if (e.target.value === "ADD_NEW") {
                        setIsCustomVehicle(true);
                        setVehicleNo("");
                      } else {
                        setVehicleNo(e.target.value);
                      }
                    }}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {savedVehicles.map((v) => (
                      <option key={v} value={v}>{v}</option>
                    ))}
                    <option value="ADD_NEW">+ Add New Vehicle</option>
                  </select>
                </div>
              ) : (
                <div className="space-y-2">
                  <input
                    type="text"
                    value={vehicleNo}
                    onChange={(e) => setVehicleNo(e.target.value)}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm uppercase focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g. WP ABC 1234"
                  />
                  {savedVehicles.length > 0 && (
                    <button
                      type="button"
                      onClick={() => {
                        setIsCustomVehicle(false);
                        setVehicleNo(savedVehicles[0]);
                      }}
                      className="text-xs text-blue-600 hover:underline"
                    >
                      ← Select from saved vehicles
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Vehicle Type Selection */}
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-2">Vehicle Type</label>
              <div className="grid grid-cols-3 gap-2">
                {VEHICLE_TYPES.map(({ value, label }) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setVehicleType(value)}
                    className={`py-2 rounded-lg border text-sm font-medium transition-colors ${
                      vehicleType === value
                        ? "bg-blue-600 text-white border-blue-600"
                        : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <button
              type="button"
              onClick={handleConfirm}
              disabled={loading}
              className="w-full bg-gray-900 hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed text-white py-3 rounded-xl text-sm font-bold transition-colors"
            >
              {loading ? "Confirming..." : "Confirm Reservation"}
            </button>
          </div>
        )}

        {step === "payment" && (
          <div className="bg-white rounded-2xl shadow-md p-6 space-y-5">
            {/* Live 2-Minute Soft Lock Countdown Display */}
            <div className={`p-4 rounded-xl border text-center transition-colors ${
              timeLeft > 30 ? "bg-amber-50 border-amber-200 text-amber-900" : "bg-red-50 border-red-200 text-red-900"
            }`}>
              <p className="text-xs font-semibold uppercase tracking-wider">Slot Soft Lock Active</p>
              <p className="text-3xl font-extrabold mt-1">{formatTimer(timeLeft)}</p>
              <p className="text-xs mt-1 text-gray-500">Pay before timer expires to guarantee your booking.</p>
            </div>

            <div className="flex items-center gap-3 bg-emerald-50 border border-emerald-200 rounded-xl px-4 py-3">
              <span className="text-emerald-500 text-xl">✓</span>
              <div>
                <p className="text-sm font-semibold text-emerald-700">Reservation Created</p>
                <p className="text-xs text-emerald-600">ID #{reservationId} — pending payment</p>
              </div>
            </div>

            <div className="flex items-center justify-between border border-gray-100 rounded-xl px-4 py-4 bg-gray-50">
              <div>
                <p className="text-sm text-gray-500">Reservation Fee</p>
                <p className="text-xs text-gray-400 mt-0.5">Secures your parking slot</p>
              </div>
              <p className="text-2xl font-black text-gray-900">Rs. 200</p>
            </div>

            <div className="text-sm text-gray-600 space-y-1">
              <div className="flex justify-between">
                <span className="text-gray-400">Vehicle</span>
                <span>{vehicleNo.toUpperCase()} · {vehicleType}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Slot</span>
                <span>{slotCode} · {hubName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Scheduled</span>
                <span>{date} at {time}</span>
              </div>
            </div>

            <button
              type="button"
              onClick={handleProceedToPayment}
              disabled={payLoading || timeLeft === 0}
              className="w-full bg-blue-600 hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed text-white py-3 rounded-xl text-sm font-bold transition-colors flex items-center justify-center gap-2"
            >
              {payLoading ? (
                <>
                  <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                  </svg>
                  Redirecting to payment…
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                  Proceed to Payment · Rs. 200
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}