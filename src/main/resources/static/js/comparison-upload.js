class ComparisonUploadManager {
    constructor() {
        this.referenceFile = null;
        this.referenceFileId = null;
        this.comparisonFiles = [];
        this.comparisonFileIds = [];
        this.referenceUploaded = false;
        this.comparisonUploaded = false;
        
        this.initializeElements();
        this.setupEventListeners();
    }

    initializeElements() {
        // 기준 파일 관련 요소들
        this.referenceUploadArea = document.getElementById('referenceUploadArea');
        this.referenceInput = document.getElementById('referenceInput');
        this.referenceSelectBtn = document.getElementById('referenceSelectBtn');
        this.referenceFileInfo = document.getElementById('referenceFileInfo');
        this.referenceFileName = document.getElementById('referenceFileName');
        this.referenceFileSize = document.getElementById('referenceFileSize');
        this.referenceFileType = document.getElementById('referenceFileType');
        this.referenceUploadBtn = document.getElementById('referenceUploadBtn');
        this.referenceFileValidation = document.getElementById('referenceFileValidation');
        this.referenceValidationMessage = document.getElementById('referenceValidationMessage');
        
        // 비교 파일들 관련 요소들
        this.comparisonUploadArea = document.getElementById('comparisonUploadArea');
        this.comparisonInput = document.getElementById('comparisonInput');
        this.comparisonSelectBtn = document.getElementById('comparisonSelectBtn');
        this.comparisonFilesList = document.getElementById('comparisonFilesList');
        this.comparisonFilesContainer = document.getElementById('comparisonFilesContainer');
        this.comparisonUploadBtn = document.getElementById('comparisonUploadBtn');
        
        // 공통 요소들
        this.analysisSection = document.getElementById('analysisSection');
        this.startComparisonBtn = document.getElementById('startComparisonBtn');
        this.resetUploadBtn = document.getElementById('resetUploadBtn');
    }

    setupEventListeners() {
        // 기준 파일 이벤트
        this.referenceInput.addEventListener('change', (e) => this.handleReferenceFileSelect(e));
        this.referenceSelectBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.referenceInput.click();
        });
        this.referenceUploadArea.addEventListener('click', (e) => {
            if (e.target === this.referenceUploadArea || e.target.closest('.upload-area') === this.referenceUploadArea) {
                if (!e.target.closest('button')) {
                    this.referenceInput.click();
                }
            }
        });
        this.referenceUploadArea.addEventListener('dragover', (e) => this.handleDragOver(e, this.referenceUploadArea));
        this.referenceUploadArea.addEventListener('dragleave', (e) => this.handleDragLeave(e, this.referenceUploadArea));
        this.referenceUploadArea.addEventListener('drop', (e) => this.handleReferenceDrop(e));
        this.referenceUploadBtn.addEventListener('click', () => this.uploadReferenceFile());
        
        // 비교 파일들 이벤트
        this.comparisonInput.addEventListener('change', (e) => this.handleComparisonFilesSelect(e));
        this.comparisonSelectBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.comparisonInput.click();
        });
        this.comparisonUploadArea.addEventListener('click', (e) => {
            if (e.target === this.comparisonUploadArea || e.target.closest('.upload-area') === this.comparisonUploadArea) {
                if (!e.target.closest('button')) {
                    this.comparisonInput.click();
                }
            }
        });
        this.comparisonUploadArea.addEventListener('dragover', (e) => this.handleDragOver(e, this.comparisonUploadArea));
        this.comparisonUploadArea.addEventListener('dragleave', (e) => this.handleDragLeave(e, this.comparisonUploadArea));
        this.comparisonUploadArea.addEventListener('drop', (e) => this.handleComparisonDrop(e));
        this.comparisonUploadBtn.addEventListener('click', () => this.uploadComparisonFiles());
        
        // 공통 버튼 이벤트
        this.startComparisonBtn.addEventListener('click', () => this.startComparison());
        this.resetUploadBtn.addEventListener('click', () => this.reset());
    }

    // 드래그 앤 드롭 이벤트 핸들러
    handleDragOver(e, area) {
        e.preventDefault();
        area.classList.add('dragover');
    }

    handleDragLeave(e, area) {
        e.preventDefault();
        area.classList.remove('dragover');
    }

    handleReferenceDrop(e) {
        e.preventDefault();
        this.referenceUploadArea.classList.remove('dragover');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            this.handleReferenceFileSelect({ target: { files: [files[0]] } });
        }
    }

    handleComparisonDrop(e) {
        e.preventDefault();
        this.comparisonUploadArea.classList.remove('dragover');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            this.handleComparisonFilesSelect({ target: { files: files } });
        }
    }

    // 기준 파일 선택 처리
    async handleReferenceFileSelect(e) {
        const file = e.target.files[0];
        if (!file) return;

        this.referenceFile = file;
        this.referenceFileId = this.generateFileId();
        
        this.displayReferenceFileInfo(file);
        await this.validateFile(file, 'reference');
    }

    displayReferenceFileInfo(file) {
        this.referenceFileName.textContent = file.name;
        this.referenceFileSize.textContent = this.formatFileSize(file.size);
        this.referenceFileType.textContent = file.type || 'FASTA';
        
        this.referenceFileInfo.classList.remove('d-none');
        this.referenceUploadArea.classList.add('active');
    }

    // 비교 파일들 선택 처리
    async handleComparisonFilesSelect(e) {
        const files = Array.from(e.target.files);
        if (files.length === 0) return;

        if (files.length > 10) {
            this.showAlert('최대 10개의 파일만 선택할 수 있습니다.', 'warning');
            return;
        }

        this.comparisonFiles = files;
        this.comparisonFileIds = files.map(() => this.generateFileId());
        
        this.displayComparisonFiles(files);
        this.comparisonFilesList.classList.remove('d-none');
        this.comparisonUploadArea.classList.add('active');
        
        // 모든 파일 검증
        let allValid = true;
        for (let file of files) {
            const isValid = await this.validateFile(file, 'comparison');
            if (!isValid) allValid = false;
        }
        
        this.comparisonUploadBtn.disabled = !allValid;
    }

    displayComparisonFiles(files) {
        this.comparisonFilesContainer.innerHTML = '';
        
        files.forEach((file, index) => {
            const fileDiv = document.createElement('div');
            fileDiv.className = 'card mb-2';
            fileDiv.innerHTML = `
                <div class="card-body py-2">
                    <div class="row align-items-center">
                        <div class="col-md-8">
                            <h6 class="mb-0" style="color: #ffffff; font-weight: 600;"><i class="bi bi-file-text" style="color: #0091ff;"></i> ${file.name}</h6>
                            <small class="text-muted">크기: ${this.formatFileSize(file.size)}</small>
                        </div>
                        <div class="col-md-4 text-end">
                            <span class="badge bg-secondary" id="status_${index}">대기중</span>
                        </div>
                    </div>
                </div>
            `;
            this.comparisonFilesContainer.appendChild(fileDiv);
        });
    }

    async validateFile(file, type) {
        try {
            const response = await fetch('/api/upload/validate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    fileName: file.name,
                    fileSize: file.size
                })
            });
            
            const result = await response.json();
            
            if (type === 'reference') {
                this.displayReferenceValidationResult(result);
            }
            
            return result.valid;
            
        } catch (error) {
            console.error('File validation error:', error);
            if (type === 'reference') {
                this.displayReferenceValidationResult({
                    valid: false,
                    message: '파일 검증 중 오류가 발생했습니다.'
                });
            }
            return false;
        }
    }

    displayReferenceValidationResult(result) {
        this.referenceValidationMessage.className = `alert ${result.valid ? 'alert-success' : 'alert-danger'}`;
        this.referenceValidationMessage.innerHTML = `
            <i class="bi bi-${result.valid ? 'check-circle' : 'exclamation-triangle'}"></i>
            ${result.message}
        `;
        this.referenceFileValidation.classList.remove('d-none');
        this.referenceUploadBtn.disabled = !result.valid;
    }

    // 기준 파일 업로드
    async uploadReferenceFile() {
        if (!this.referenceFile) return;
        
        this.referenceUploadBtn.disabled = true;
        this.referenceUploadBtn.textContent = '업로드 중...';
        
        try {
            await this.uploadSingleFile(this.referenceFile, this.referenceFileId);
            this.referenceUploaded = true;
            this.referenceUploadBtn.innerHTML = '<i class="bi bi-check-circle"></i> 업로드 완료';
            this.referenceUploadBtn.className = 'btn btn-success';
            this.showAlert('기준 파일 업로드가 완료되었습니다.', 'success');
            this.checkAllUploadsComplete();
        } catch (error) {
            console.error('Reference file upload error:', error);
            this.showAlert('기준 파일 업로드 중 오류가 발생했습니다: ' + error.message, 'danger');
            this.referenceUploadBtn.disabled = false;
            this.referenceUploadBtn.textContent = '업로드';
        }
    }

    // 비교 파일들 업로드
    async uploadComparisonFiles() {
        if (this.comparisonFiles.length === 0) return;
        
        this.comparisonUploadBtn.disabled = true;
        this.comparisonUploadBtn.textContent = '업로드 중...';
        
        try {
            for (let i = 0; i < this.comparisonFiles.length; i++) {
                const file = this.comparisonFiles[i];
                const fileId = this.comparisonFileIds[i];
                
                // 상태 업데이트
                const statusElement = document.getElementById(`status_${i}`);
                statusElement.className = 'badge bg-warning';
                statusElement.textContent = '업로드 중...';
                
                await this.uploadSingleFile(file, fileId);
                
                // 완료 상태 업데이트
                statusElement.className = 'badge bg-success';
                statusElement.textContent = '완료';
            }
            
            this.comparisonUploaded = true;
            this.comparisonUploadBtn.innerHTML = '<i class="bi bi-check-circle"></i> 모든 파일 업로드 완료';
            this.comparisonUploadBtn.className = 'btn btn-success';
            this.showAlert('모든 비교 파일 업로드가 완료되었습니다.', 'success');
            this.checkAllUploadsComplete();
        } catch (error) {
            console.error('Comparison files upload error:', error);
            this.showAlert('비교 파일 업로드 중 오류가 발생했습니다: ' + error.message, 'danger');
            this.comparisonUploadBtn.disabled = false;
            this.comparisonUploadBtn.textContent = '모든 파일 업로드';
        }
    }

    async uploadSingleFile(file, fileId) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fileId', fileId);
        
        const response = await fetch('/api/upload/single', {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        if (!result.success) {
            throw new Error(result.message);
        }
        
        return result;
    }

    checkAllUploadsComplete() {
        if (this.referenceUploaded && this.comparisonUploaded && 
            this.referenceFile && this.comparisonFiles.length > 0) {
            this.analysisSection.classList.remove('d-none');
        }
    }

    startComparison() {
        if (!this.referenceFileId || this.comparisonFileIds.length === 0) {
            this.showAlert('기준 파일과 비교 파일들이 모두 업로드되어야 합니다.', 'warning');
            return;
        }
        
        // 분석 페이지로 이동
        const params = new URLSearchParams();
        params.append('referenceId', this.referenceFileId);
        this.comparisonFileIds.forEach(id => params.append('comparisonIds', id));
        
        window.location.href = '/comparison-analysis?' + params.toString();
    }

    reset() {
        this.referenceFile = null;
        this.referenceFileId = null;
        this.comparisonFiles = [];
        this.comparisonFileIds = [];
        this.referenceUploaded = false;
        this.comparisonUploaded = false;
        
        // UI 초기화
        this.referenceFileInfo.classList.add('d-none');
        this.referenceFileValidation.classList.add('d-none');
        this.comparisonFilesList.classList.add('d-none');
        this.analysisSection.classList.add('d-none');
        this.referenceUploadArea.classList.remove('active');
        this.comparisonUploadArea.classList.remove('active');
        
        // 입력 필드 초기화
        this.referenceInput.value = '';
        this.comparisonInput.value = '';
        
        // 버튼 상태 초기화
        this.referenceUploadBtn.disabled = false;
        this.referenceUploadBtn.textContent = '업로드';
        this.referenceUploadBtn.className = 'btn btn-warning';
        this.comparisonUploadBtn.disabled = true;
        this.comparisonUploadBtn.textContent = '모든 파일 업로드';
        this.comparisonUploadBtn.className = 'btn btn-success';
    }

    // 유틸리티 함수들
    generateFileId() {
        return Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    showAlert(message, type) {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.querySelector('.container').insertBefore(alertDiv, document.querySelector('.container').firstChild);
        
        // 5초 후 자동 제거
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.parentNode.removeChild(alertDiv);
            }
        }, 5000);
    }
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    new ComparisonUploadManager();
});