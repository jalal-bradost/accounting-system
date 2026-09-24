# HR End-to-End Scenario — Acceptance Test Checklist

Use with `hr.seed.demo-data=true` and demo payroll/attendance/time-off seeders enabled.

## Setup

1. Start backend and frontend; log in as `admin`.
2. Confirm currencies in **Accounting → Configuration → Currencies** (not hardcoded IQD in UI).
3. Create 5 platform users in **Settings → Users** (or use demo users if seeded).
4. Assign roles: `EMPLOYEE` for staff, `HR_MANAGER` for HR account.

## Employee & contracts

5. Create 5 employees in **HR → Employees**; link each to a platform user via **Linked user**.
6. Create payroll structures/schedules if not seeded.
7. Create **running** contracts per employee; pick currency via **Currency** dropdown.

## Attendance

8. Open **Attendances** → use **Bulk fill** for the month from working schedules.

## Time off

9. Log in as employee → **My Time Off** → submit leave requests.
10. Log in as HR → **Time Off** → approve/reject; check team summary table.
11. Create allocations via **Time Off → Allocations → New** if needed.

## Payroll & accounting

12. **Payroll → Pay Runs** → create run for month → **Compute** → **Post** → **Pay**.
13. Verify journal entries in **Accounting → Journal Entries**.
14. Employees view payslips at **My Payslips**.

## Leave balances

15. HR views per-employee and team leave stats: allocated, used, pending, remaining.
