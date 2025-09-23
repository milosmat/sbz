import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth-guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent) },
  { path: '', canActivate: [AuthGuard], children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./pages/feed/feed.component').then(m => m.FeedComponent) },
      { path: 'me/posts', loadComponent: () => import('./pages/my-posts/my-posts.component').then(m => m.MyPostsComponent) },
      { path: 'new', loadComponent: () => import('./pages/new-post/new-post.component').then(m => m.NewPostComponent) },
      { path: 'friends/search', loadComponent: () => import('./pages/friends-search/friends-search.component').then(m => m.FriendsSearchComponent) },
      { path: 'places/new', loadComponent: () => import('./pages/new-place/new-place.component').then(m => m.NewPlaceComponent) },
      { path: 'feed', loadComponent: () => import('./pages/feed/feed.component').then(m => m.FeedComponent) },
      { path: 'admin/moderation', loadComponent: () => import('./pages/admin-moderation/admin-moderation.component').then(m => m.AdminModerationComponent) },
      // NOVO: reklame (stranica)
      { path: 'ads', loadComponent: () => import('./pages/recommended-ads/recommended-ads.component').then(m => m.RecommendedAdsComponent) },

      // NOVO: mesta (lista i detalj)
      { path: 'places', loadComponent: () => import('./pages/places-list/places-list.component').then(m => m.PlacesListComponent) },
      { path: 'places/:id', loadComponent: () => import('./pages/place-detail/place-detail.component').then(m => m.PlaceDetailComponent) },

    ]
  },
  { path: '**', redirectTo: '' }
];
