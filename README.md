# Enso

A personal finance app for Android that helps me track my expenses and manage my money day to day. It automatically imports transactions from MTN MoMo and Airtel Money by reading SMS confirmations.

experimental building.

---

## Why

MTN and Airtel don't expose a developer API for personal transaction data. Your options as a user are to export a statement from the MoMo app, or browse your history inside the app itself neither of which gives you any freedom to actually work with the data.SMS confirmations are sent for every transaction anyway.

I wanted to slice it however I want. See spending by category, by week, by transaction type. Understand where my money actually goes. SMS confirmations are sent for every transaction anyway, so Enso reads those and builds the history automatically no exports, no manual entry, no app switching.

## What It Does

- Automatically imports MTN MoMo and Airtel Money transactions from SMS
- Tracks all transaction types: withdrawals, deposits, transfers, merchant payments, bundle purchases, MoMo Advance, loan collections, and electricity payments
- Manual entry for cash transactions that don't go through mobile money
- Spending summaries and breakdowns by category and time period
- Full control over your own financial data

## Stack

- Kotlin + Jetpack Compose
- Room (local database)
- Material 3
- Requires Android 8.0+

## Notes

- Physical device only — SMS access doesn't work on emulators
- Everything stays on-device, nothing leaves your phone
- Built as a hobby project, with help from Claude

## Status

Work in progress. Building it as I use it.
