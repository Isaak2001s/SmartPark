# SmartPark 🅿️

Εφαρμογή Έξυπνης Πόλης για Διαχείριση Στάθμευσης στην Αθήνα — CN6008 CW1.

**Φοιτητής:** Ισαάκ Αγιάνογλου (2678437)  
**Μάθημα:** CN6008 – Advanced Topics in Computer Science  
**Πανεπιστήμιο:** University of East London

## Τεχνολογική Στοίβα

- Kotlin 2.2.10 + Jetpack Compose
- Firebase Authentication (email/password)
- Cloud Firestore (NoSQL βάση δεδομένων πραγματικού χρόνου)
- Google Maps SDK for Android
- Αρχιτεκτονική MVVM + Repository Pattern

## Εγκατάσταση

1. Δημιουργήστε ένα Firebase project και προσθέστε Android app με package `com.example.smartpark`
2. Κατεβάστε το `google-services.json` και τοποθετήστε το στον φάκελο `app/`
3. Ενεργοποιήστε **Authentication** (Email/Password) και **Cloud Firestore** στο Firebase Console
4. Ενεργοποιήστε **Maps SDK for Android** στο Google Cloud Console
5. Δημιουργήστε ένα Maps API key και προσθέστε το στο `local.properties`:
   ```
   MAPS_API_KEY=το_κλειδί_σας_εδώ
   ```
6. Εισάγετε δεδομένα στάθμευσης στη Firestore (δείτε τη δομή `parking_areas` στην αναφορά)
7. Ανοίξτε το project στο Android Studio και πατήστε Run σε συσκευή ή emulator (API 26+)

## Λειτουργίες

- Εγγραφή και σύνδεση χρήστη με μόνιμη συνεδρία
- Google Maps με διαθεσιμότητα στάθμευσης σε πραγματικό χρόνο
- Χρωματικοί markers (πράσινο/κίτρινο/κόκκινο ανάλογα με πληρότητα)
- Πάτημα σε marker → λεπτομέρειες χώρου
- Κράτηση θέσης (ατομικό Firestore transaction)
- Οθόνη «Οι Κρατήσεις μου» (Ενεργές / Ιστορικό)
- Ακύρωση κράτησης με αποκατάσταση θέσης
- Κανόνες ασφαλείας Firestore (έλεγχος πρόσβασης σε επίπεδο πεδίου)
