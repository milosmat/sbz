import { User } from "./user";

export interface Post {
  id: string;
  authorId: string;
  text: string;
  hashtags: string[];
  likes: number;
  reports: number;
  createdAtEpochMs: number;
}
