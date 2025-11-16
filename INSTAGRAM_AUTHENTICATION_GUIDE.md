# Instagram Authentication Guide

## Problem

Some Instagram content (private accounts, stories, or posts from accounts you follow) requires
authentication. You might see this error:

```
Instagram Authentication Required

This content requires login. To download:
...
```

## Solution: Browser Cookie Authentication

The app can extract cookies from your browser to authenticate with Instagram. This allows you to
download private content that you have access to.

### Step-by-Step Instructions

#### 1. **Log into Instagram in Your Browser**

- Open your preferred browser (Chrome, Firefox, Edge, or Safari)
- Go to https://www.instagram.com
- Log in with your Instagram account
- Navigate to the content you want to download to verify you can see it

#### 2. **Configure the App**

1. Open James Music Converter
2. Paste the Instagram URL
3. Tap on **"Advanced Options"** to expand it
4. Enable the checkbox: **"Extract cookies from browser"**
5. In the "Browser name" field, enter one of:
    - `chrome` (for Google Chrome)
    - `firefox` (for Mozilla Firefox)
    - `edge` (for Microsoft Edge)
    - `safari` (for Safari on Mac)

#### 3. **Start Download**

- Tap the download button
- The app will extract your login cookies from the specified browser
- Your download should now work!

### Supported Browsers

| Browser | Enter This |
|---------|-----------|
| Google Chrome | `chrome` |
| Mozilla Firefox | `firefox` |
| Microsoft Edge | `edge` |
| Safari (Mac) | `safari` |
| Brave | `brave` |
| Opera | `opera` |

### Requirements

**Important:** For cookie extraction to work:

1. You must be logged into Instagram in the specified browser
2. The browser must be installed on your device
3. You should keep the browser logged in

### How It Works

The app uses yt-dlp's `--cookies-from-browser` feature to:

1. Access your browser's cookie storage (locally on your device)
2. Extract Instagram authentication cookies
3. Use those cookies to authenticate the download request

**Privacy Note:** The cookies never leave your device. The app only uses them to authenticate with
Instagram on your behalf.

### Troubleshooting

#### "Browser not found" or "No cookies found"

- Make sure the browser is installed on your device
- Check that you spelled the browser name correctly
- Try logging out and back into Instagram in that browser
- Try a different browser

#### "Authentication failed" after using cookies

- Clear your browser cookies and log into Instagram again
- Make sure you're using the correct browser name
- Try using a different browser
- Check if Instagram changed their login requirements

#### Still not working?

- Try downloading a public Instagram post first to verify the app works
- Check if the content is still available (not deleted)
- Verify you have access to the content when logged in via browser
- Try using username/password authentication instead (see below)

### Alternative: Username/Password Authentication

For some platforms, you can also use username and password:

1. Expand **"Advanced Options"**
2. Enter your Instagram **Username**
3. Enter your Instagram **Password**
4. Start the download

**Note:** Username/password authentication may not work on all platforms due to 2FA and other
security measures. Browser cookies are generally more reliable.

### Other Platforms That May Require Authentication

This feature works for any platform that requires login:

- **Instagram**: Private accounts, stories, follower-only content
- **Twitter/X**: Protected accounts, login-walled content
- **Facebook**: Private videos, friend-only content
- **TikTok**: Private accounts (in some regions)
- **YouTube**: Private or unlisted videos (with proper access)

### Security Considerations

1. **Cookie Safety**: Cookies are extracted directly from your browser's local storage. They never
   go through any external servers.

2. **Access Scope**: The app only gets the cookies needed for the specific site (e.g., Instagram
   cookies for Instagram downloads).

3. **Session Duration**: Cookies typically expire after some time (hours to days). You may need to
   log in again in your browser if cookies expire.

4. **Two-Factor Authentication**: If your account uses 2FA, make sure you're logged in to the
   browser first. The cookie contains the session token after successful 2FA verification.

### Example Workflow

Let's say you want to download a story from a private Instagram account you follow:

1. **Open Chrome** → Go to instagram.com → Log in → View the story
2. **Copy the story URL** (tap Share → Copy Link)
3. **Open James Music Converter**
4. **Paste the URL**
5. **Expand Advanced Options**
6. **Enable "Extract cookies from browser"**
7. **Type "chrome"** in the browser name field
8. **Choose Video or Audio mode**
9. **Tap Download**
10. **Done!** The app authenticates using your Chrome cookies

### Best Practices

✅ **Do:**

- Keep your browser logged into Instagram
- Use the same browser consistently
- Update your browser regularly
- Log out and back in if cookies expire

❌ **Don't:**

- Share your cookies with anyone
- Use public/shared computers for cookie extraction
- Use the app on devices you don't trust

### Need More Help?

If you're still having issues:

1. Check that the Instagram content is actually accessible when you're logged in via browser
2. Try a different browser
3. Try a public Instagram post first to verify the app works
4. Check the app logs for detailed error messages
5. Report issues with specific error messages on the project's GitHub

---

**Last Updated:** November 2024
**App Version:** 1.0+
