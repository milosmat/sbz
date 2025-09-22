import { User } from "./user";

export interface Post {
  id: string;
  authorId: string;
  text: string;
  createdAt: number;
  likes: number;
  author?: User;
}
