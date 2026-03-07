<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="IE=Edge">
  <!--
    GradeCalc — Student grading calculator
    Flutter Web entry point.

    WEB-SPECIFIC CONFIGURATION:
    • viewport meta prevents mobile browsers from auto-zooming text fields
    • theme-color matches the app's kNavy900 (#060C18) for browser chrome
    • Removed flutter_bootstrap.js reference — using standard FlutterLoader
  -->
  <meta name="theme-color" content="#060C18">
  <meta name="description" content="GradeCalc – import Excel grade sheets, apply weighted scoring, export results">
  <link rel="icon" type="image/png" href="favicon.png">
  <link rel="manifest" href="manifest.json">

  <title>GradeCalc</title>

  <style>
    /* Prevent flash of white before Flutter canvas is ready */
    html, body {
      margin: 0;
      padding: 0;
      background-color: #060C18;
      color: #E8EDF8;
      height: 100%;
      overflow: hidden;
    }

    /* Loading indicator shown before Flutter engine initialises */
    #loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      gap: 20px;
      font-family: system-ui, -apple-system, sans-serif;
    }

    #loading .logo {
      width: 48px;
      height: 48px;
      background: #2979FF;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      font-weight: 900;
      color: white;
    }

    #loading p {
      color: #7B93BB;
      font-size: 14px;
      margin: 0;
    }

    /* Spinner */
    #loading .spinner {
      width: 28px;
      height: 28px;
      border: 3px solid #112448;
      border-top-color: #2979FF;
      border-radius: 50%;
      animation: spin 0.9s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }
  </style>
</head>
<body>
  <!-- Pre-Flutter loading screen -->
  <div id="loading">
    <div class="logo">G</div>
    <div class="spinner"></div>
    <p>Loading GradeCalc…</p>
  </div>

  <script>
    // Remove the loading overlay once Flutter signals it has mounted
    window.addEventListener('flutter-first-frame', function() {
      const el = document.getElementById('loading');
      if (el) el.style.display = 'none';
    });
  </script>

  <script src="flutter_bootstrap.js" async></script>
</body>
</html>
