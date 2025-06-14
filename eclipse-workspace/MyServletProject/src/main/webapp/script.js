function showRegister() {
    document.getElementById('loginForm').style.display = 'none';
    document.getElementById('registerForm').style.display = 'block';
    clearMessages();
}

function showLogin() {
    document.getElementById('registerForm').style.display = 'none';
    document.getElementById('loginForm').style.display = 'block';
    clearMessages();
}

function clearMessages() {
    const resultDiv = document.getElementById('result');
    resultDiv.innerHTML = '';
}

document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('login');
    const registerForm = document.getElementById('register');
    const resultDiv = document.getElementById('result');

    if (!loginForm || !registerForm || !resultDiv) {
        console.error('Form or resultDiv elements not found');
        return;
    }

    loginForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const username = document.getElementById('loginUsername').value.trim();
        const password = document.getElementById('loginPassword').value.trim();
        if (username === '' || password === '') {
            showMessage('Please enter both username and password.', 'error');
            return;
        }
        await sendRequest('login', username, password);
    });

    registerForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const username = document.getElementById('registerUsername').value.trim();
        const password = document.getElementById('registerPassword').value.trim();
        if (username === '' || password === '') {
            showMessage('Please enter both username and password.', 'error');
            return;
        }
        await sendRequest('register', username, password);
    });

	async function sendRequest(action, username, password) {
	    try {
	        const response = await fetch('AuthServlet', {
	            method: 'POST',
	            headers: {
	                'Content-Type': 'application/x-www-form-urlencoded',
	            },
	            body: `action=${action}&username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
	        });
	        const contentType = response.headers.get('content-type');
	        let data;
	        
	        if (contentType && contentType.includes('application/json')) {
	            data = await response.json();
	        } else {
	            const text = await response.text();
	            data = { html: text };
	        }

	        if (!response.ok) {
	            throw new Error('Phản hồi không hợp lệ');
	        }

	        if (data.redirect) {
	            window.location.href = data.redirect;
	            return;
	        }

	        if (data.success) {
	            showMessage(data.message, 'success');
	            if (action === 'register') {
	                showLogin();
	            }
	        } else if (data.html) {
	            resultDiv.innerHTML = data.html;
	        }
	    } catch (error) {
	        showMessage('Lỗi: ' + error.message, 'error');
	    }
	}
	
	
    function showMessage(message, type) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.textContent = message;
        messageDiv.style.padding = '10px';
        messageDiv.style.marginTop = '10px';
        messageDiv.style.borderRadius = '4px';
        messageDiv.style.color = 'white';
        messageDiv.style.backgroundColor = type === 'error' ? '#dc3545' : '#28a745';
        resultDiv.appendChild(messageDiv);
        setTimeout(() => messageDiv.remove(), 3000);
    }
});