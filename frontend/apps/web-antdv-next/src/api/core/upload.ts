import { requestClient } from '#/api/request';

export interface UploadedImage {
  key: string;
  url: string;
}

export const uploadImage = (file: File) =>
  requestClient.upload<UploadedImage>('/upload', { file });

interface UploadFileParams {
  file: File;
  onError?: (error: Error) => void;
  onProgress?: (progress: { percent: number }) => void;
  onSuccess?: (data: UploadedImage, file: File) => void;
}
export async function upload_file({
  file,
  onError,
  onProgress,
  onSuccess,
}: UploadFileParams) {
  try {
    onProgress?.({ percent: 0 });

    const data = await uploadImage(file);

    onProgress?.({ percent: 100 });
    onSuccess?.(data, file);
  } catch (error) {
    onError?.(error instanceof Error ? error : new Error(String(error)));
  }
}
