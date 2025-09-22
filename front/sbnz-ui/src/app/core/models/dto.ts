export interface RegisterRequest {
  firstName: string; lastName: string; email: string; password: string; city?: string;
}
export interface LoginRequest { email: string; password: string; }
export interface CreatePostRequest { authorId: string; text: string; }
export interface LikePostRequest { userId: string; postId: string; }

export interface Paged<T> { items: T[]; total: number; }
